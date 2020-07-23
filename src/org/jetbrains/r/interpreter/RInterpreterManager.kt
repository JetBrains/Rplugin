/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.psi.stubs.StubUpdatingIndex
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.rejectedPromise
import org.jetbrains.r.RBundle
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.packages.remote.RepoProvider
import org.jetbrains.r.packages.remote.ui.RInstalledPackagesPanel
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.settings.RInterpreterSettings
import org.jetbrains.r.settings.RSettings
import org.jetbrains.r.statistics.RStatistics
import java.io.IOException
import java.nio.file.Paths

interface RInterpreterManager {
  val interpreterOrNull: RInterpreter?
  val interpreterLocation: RInterpreterLocation?

  fun getInterpreterAsync(force: Boolean = false): Promise<RInterpreter>

  fun getInterpreterBlocking(timeout: Int) = getInterpreterAsync().run {
    try {
      blockingGet(timeout)
    } catch (t: Throwable) {
      null
    }
  }

  fun hasInterpreter(): Boolean

  companion object {
    fun getInstance(project: Project): RInterpreterManager = project.getService(RInterpreterManager::class.java)
    fun getInstanceIfCreated(project: Project): RInterpreterManager? = project.getServiceIfCreated(RInterpreterManager::class.java)

    @JvmOverloads
    fun getInterpreterAsync(project: Project, force: Boolean = false): Promise<RInterpreter> = getInstance(project).getInterpreterAsync(force)

    fun getInterpreterOrNull(project: Project): RInterpreter? = getInstanceIfCreated(project)?.interpreterOrNull
    fun getInterpreterBlocking(project: Project, timeout: Int): RInterpreter? = getInstance(project).getInterpreterBlocking(timeout)

    fun restartInterpreter(project: Project) {
      getInterpreterAsync(project, true).onProcessed { interpreter ->
        if (interpreter != null) {
          RepoProvider.getInstance(project).onInterpreterVersionChange()
          ApplicationManager.getApplication().invokeLater {
            val packagesPanel = RToolWindowFactory.findContent(project, RToolWindowFactory.PACKAGES).component as RInstalledPackagesPanel
            packagesPanel.scheduleRefresh()
          }
        }
        RConsoleManager.getInstance(project).currentConsoleAsync.onSuccess {
          runInEdt {
            RConsoleManager.closeMismatchingConsoles(project, interpreter)
            RConsoleToolWindowFactory.getRConsoleToolWindows(project)?.show {}
          }
        }
      }
    }
  }
}

class RInterpreterManagerImpl(private val project: Project): RInterpreterManager {
  @Volatile
  private var interpreterPromise: Promise<RInterpreter> = rejectedPromise("No R Interpreter")
  @Volatile
  private var initialized = false

  @Volatile
  override var interpreterOrNull: RInterpreterBase? = null
    private set(value) {
      field?.onUnsetAsProjectInterpreter()
      field = value
      value?.onSetAsProjectInterpreter()
    }

  @Volatile
  override var interpreterLocation = fetchInterpreterLocation()
    private set

  init {
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      invalidateRSkeletonCaches()
      removeLocalRDataTmpFiles()
    }
  }

  override fun getInterpreterAsync(force: Boolean): Promise<RInterpreter> {
    if (initialized && !force) return interpreterPromise
    synchronized(this) {
      if (initialized && !force) return interpreterPromise
      if (force) {
        interpreterOrNull = null
        interpreterLocation = fetchInterpreterLocation()
      }
      val location = interpreterLocation
                     ?: return rejectedPromise<RInterpreter>("No R Interpreter").also { interpreterPromise = it }
      if (!initialized) {
        RLibraryWatcher.subscribe(project, RLibraryWatcher.TimeSlot.FIRST) {
          RInterpreterStateManager.getCurrentStateAsync(project).onSuccess { it.scheduleSkeletonUpdate() }.then { }
        }
      }
      initialized = true
      return setupInterpreter(location)
        .onError { RSettings.getInstance(project).interpreterLocation = null }
        .also { interpreterPromise = it }
    }
  }

  override fun hasInterpreter() = RSettings.getInstance(project).interpreterLocation != null

  private fun fetchInterpreterLocation(): RInterpreterLocation? {
    return if (ApplicationManager.getApplication().isUnitTestMode) {
      RLocalInterpreterLocation(RInterpreterUtil.suggestHomePath())
    } else {
      RSettings.getInstance(project).interpreterLocation
    }
  }

  private fun ensureInterpreterStored(interpreter: RInterpreter) {
    val info = RBasicInterpreterInfo(SUGGESTED_INTERPRETER_NAME, interpreter.interpreterLocation, interpreter.version)
    RInterpreterSettings.addOrEnableInterpreter(info)
  }

  private fun setupInterpreter(location: RInterpreterLocation): Promise<RInterpreter> {
    val promise = AsyncPromise<RInterpreter>()
    runBackgroundableTask("Initializing R interpreter", project) {
      try {
        location.createInterpreter(project).let {
          interpreterOrNull = it
          ensureInterpreterStored(it)
          RInterpreterStateManager.getCurrentStateAsync(project).onSuccess { state -> state.scheduleSkeletonUpdate() }
          promise.setResult(it)
          RStatistics.logSetupInterpreter(it)
        }
      } catch (e: Throwable) {
        promise.setError(e)
        RInterpreterBase.LOG.warn(e)
      }
    }
    return promise
  }

  private fun removeLocalRDataTmpFiles() {
    try {
      val dir = Paths.get(project.basePath ?: return, ".RDataFiles").toFile()
      if (dir.isDirectory) {
        dir.listFiles()?.filter { ".RDataTmp" in it.name }?.forEach { it.delete() }
      }
    } catch (e: IOException) {
      RInterpreterBase.LOG.error(e)
    }
  }

  companion object {
    private const val DOWNLOAD_R_PAGE = "https://cloud.r-project.org/"

    private val SUGGESTED_INTERPRETER_NAME = RBundle.message("project.settings.suggested.interpreter")

    fun openDownloadRPage() {
      BrowserLauncher.instance.browse(DOWNLOAD_R_PAGE)
    }
  }
}

/**
 * This method invalidate Stub Index on first start of R plugin
 * We need to do that to workaround IntelliJ 2020.1 platform bug when
 * skeleton files are not re-indexed on plugin update.
 */
private fun invalidateRSkeletonCaches() {
  val key = "rplugin.version"
  val version = RPluginUtil.getPlugin().version
  val propertiesComponent = PropertiesComponent.getInstance()
  val lastVersion = propertiesComponent.getValue(key)
  if (version != lastVersion) {
    propertiesComponent.setValue(key, version)
    FileBasedIndex.getInstance().requestRebuild(StubUpdatingIndex.INDEX_ID)
  }
}
