/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.psi.stubs.StubUpdatingIndex
import com.intellij.util.indexing.FileBasedIndex
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.asPromise
import org.jetbrains.r.RBundle
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.packages.remote.RepoProvider
import org.jetbrains.r.packages.remote.ui.RInstalledPackagesPanel
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.rinterop.RInteropCoroutineScope
import org.jetbrains.r.settings.RInterpreterSettings
import org.jetbrains.r.settings.RSettings
import org.jetbrains.r.statistics.RInterpretersCollector
import java.io.IOException
import java.nio.file.Paths

interface RInterpreterManager {
  val interpreterOrNull: RInterpreter?
  var interpreterLocation: RInterpreterLocation?

  /**
   * better to use [launchInterpreter] or [awaitInterpreter]
   */
  fun getInterpreterDeferred(force: Boolean = false): Deferred<Result<RInterpreter>>

  fun launchInterpreter(force: Boolean = false) {
    getInterpreterDeferred(force)
  }

  suspend fun awaitInterpreter(force: Boolean = false): Result<RInterpreter> =
    getInterpreterDeferred(force).await()

  fun hasInterpreter(): Boolean

  companion object {
    fun getInstance(project: Project): RInterpreterManager = project.service()
    private fun getInstanceIfCreated(project: Project): RInterpreterManager? = project.getServiceIfCreated(RInterpreterManager::class.java)

    @Deprecated("use RInterpreterManager.getInstance(project).getInterpreterDeferred(force) instead")
    @JvmOverloads
    fun getInterpreterAsync(project: Project, force: Boolean = false): Promise<RInterpreter> =
      getInstance(project).getInterpreterDeferred(force).asCompletableFuture().asPromise().then<RInterpreter>{ it.getOrThrow() }

    fun getInterpreterOrNull(project: Project): RInterpreter? = getInstanceIfCreated(project)?.interpreterOrNull

    fun getInterpreterBlocking(project: Project, timeout: Int): RInterpreter? =
      runBlockingCancellable {
        withTimeoutOrNull(timeout.toLong()) {
          getInstance(project).awaitInterpreter(force = false).getOrNull()
        }
      }

    fun restartInterpreter(project: Project, afterRestart: Runnable? = null) {
      val manager = getInstance(project)

      RInteropCoroutineScope.getCoroutineScope(project).launch {
        val interpreter = manager.awaitInterpreter(force = true).getOrNull()
        if (interpreter != null) {
          RepoProvider.getInstance(project).onInterpreterVersionChange()
          launch(Dispatchers.EDT) {
            val packagesPanel = RToolWindowFactory.findContent(project, RToolWindowFactory.PACKAGES).component as RInstalledPackagesPanel
            packagesPanel.scheduleRefresh()
          }
        }
        RConsoleManager.getInstance(project).awaitCurrentConsole().onSuccess {
          withContext(Dispatchers.EDT) {
            RConsoleManager.closeMismatchingConsoles(project, interpreter)
            RConsoleToolWindowFactory.getRConsoleToolWindows(project)?.show(afterRestart)
          }
        }
      }
    }
  }
}

private class NoRInterpreterException(message: String = "No R Interpreter"): RuntimeException(message)

class RInterpreterManagerImpl(
  private val project: Project,
  private val coroutineScope: CoroutineScope,
): RInterpreterManager {
  @Volatile
  private var interpreterDeferred: Deferred<Result<RInterpreter>> = CompletableDeferred(Result.failure<RInterpreter>(NoRInterpreterException()))
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

  init {
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      invalidateRSkeletonCaches()
      removeLocalRDataTmpFiles()
    }
  }

  override fun getInterpreterDeferred(force: Boolean): Deferred<Result<RInterpreter>> {
    synchronized(this) {
      if (initialized && !force) return interpreterDeferred
      if (force) {
        interpreterOrNull = null
        interpreterLocation = fetchInterpreterLocation()
      }
      val location = interpreterLocation
                     ?: return CompletableDeferred(Result.failure<RInterpreter>(NoRInterpreterException())).also { interpreterDeferred = it }
      if (!initialized) {
        RLibraryWatcher.subscribeAsync(project, RLibraryWatcher.TimeSlot.FIRST) { roots ->
          val states = RInterpreterStateManager.getInstance(project).states
          val statesRoots = states.map { state -> state.libraryPaths.map { it.path } }
          for (i in states.indices) {
            val state = states[i]
            val stateRoots = statesRoots[i]
            if (roots.any { stateRoots.contains(it) }) {
              state.scheduleSkeletonUpdate()
            }
          }
        }
      }
      initialized = true

      val deferred = coroutineScope.async {
        setupInterpreter(location)
      }

      interpreterDeferred = deferred
      return deferred
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

  private suspend fun setupInterpreter(location: RInterpreterLocation): Result<RInterpreter> {
    return withBackgroundProgress(project, RBundle.message("initializing.r.interpreter.message")) {
      location.createInterpreter(project).onSuccess { it ->
        interpreterOrNull = it
        ensureInterpreterStored(it)
        RInterpretersCollector.logSetupInterpreter(project, it)
      }.onFailure { e ->
        RInterpreterBase.LOG.warn(e)
        RSettings.getInstance(project).interpreterLocation = null
      }
    }
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
