/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.DumbModeTask
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.impl.PsiDocumentManagerImpl
import com.intellij.psi.stubs.StubUpdatingIndex
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.rejectedPromise
import org.jetbrains.r.RBundle
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.configuration.RSettingsProjectConfigurable
import org.jetbrains.r.notifications.RNotificationUtil
import org.jetbrains.r.packages.RSkeletonUtil
import org.jetbrains.r.settings.RInterpreterSettings
import org.jetbrains.r.settings.RSettings
import org.jetbrains.r.statistics.RStatistics
import java.io.IOException
import java.nio.file.Paths

interface RInterpreterManager {
  /**
   *  true if skeletons update was performed at least once.
   */
  val isSkeletonInitialized: Boolean
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
  }
}

class RInterpreterManagerImpl(private val project: Project): RInterpreterManager {
  @Volatile
  override var isSkeletonInitialized: Boolean = false
    private set
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
          scheduleSkeletonUpdate()
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

  private fun checkInterpreterLocation(location: RInterpreterLocation?): Boolean {
    val (isViable, e) = try {
      Pair(location?.getVersion() != null, null)
    } catch (e: Exception) {
      Pair(false, e)
    }
    if (!isViable) {
      val message = createInvalidLocationErrorMessage(location, e?.message)
      val settingsAction = RNotificationUtil.createNotificationAction(GO_TO_SETTINGS_HINT) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, RSettingsProjectConfigurable::class.java)
      }
      val downloadAction = RNotificationUtil.createNotificationAction(DOWNLOAD_R_HINT) {
        openDownloadRPage()
      }
      RNotificationUtil.notifyInterpreterError(project, message, settingsAction, downloadAction)
    }
    return isViable
  }

  private fun ensureInterpreterStored(interpreter: RInterpreter) {
    val info = RBasicInterpreterInfo(SUGGESTED_INTERPRETER_NAME, interpreter.interpreterLocation, interpreter.version)
    RInterpreterSettings.addOrEnableInterpreter(info)
  }

  private fun setupInterpreter(location: RInterpreterLocation): Promise<RInterpreter> {
    val promise = AsyncPromise<RInterpreter>()
    runBackgroundableTask("Initializing R interpreter", project) {
      if (!checkInterpreterLocation(location)) {
        promise.setError("Invalid R Interpreter")
        return@runBackgroundableTask
      }
      try {
        location.createInterpreter(project).let {
          interpreterOrNull = it
          ensureInterpreterStored(it)
          scheduleSkeletonUpdate()
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

  private fun scheduleSkeletonUpdate(): Promise<Unit> {
    return AsyncPromise<Unit>().also { promise ->
      val interpreter = interpreterOrNull
      if (interpreter != null) {
        interpreter.updateState()
          .onProcessed { promise.setResult(Unit) }
          .onSuccess {
            val updater = object : Task.Backgroundable(project, "Update skeletons", false) {
              override fun run(indicator: ProgressIndicator) {
                interpreter.registersRootsToWatch()
                updateSkeletons(interpreter)
              }
            }
            ProgressManager.getInstance().run(updater)
          }
      } else {
        promise.setResult(Unit)
      }
    }
  }

  private fun updateSkeletons(interpreter: RInterpreterBase) {
    val dumbModeTask = object : DumbModeTask(interpreter) {
      override fun performInDumbMode(indicator: ProgressIndicator) {
        if (!project.isOpen || project.isDisposed) return
        if (RSkeletonUtil.updateSkeletons(interpreter, indicator)) {
          runInEdt { runWriteAction { refreshSkeletons(interpreter) } }
        }
        isSkeletonInitialized = true
        RInterpreterUtil.updateIndexableSet(project)
      }
    }
    DumbService.getInstance(project).queueTask(dumbModeTask)
  }

  private fun refreshSkeletons(interpreter: RInterpreterBase) {
    if (!project.isOpen || project.isDisposed) return
    interpreter.skeletonPaths.forEach { skeletonPath ->
      val libraryRoot = LocalFileSystem.getInstance().refreshAndFindFileByPath(skeletonPath) ?: return@forEach
      VfsUtil.markDirtyAndRefresh(false, true, true, libraryRoot)
      WriteAction.runAndWait<Exception> { PsiDocumentManagerImpl.getInstance(project).commitAllDocuments() }
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
    private val GO_TO_SETTINGS_HINT = RBundle.message("interpreter.manager.go.to.settings.hint")
    private val DOWNLOAD_R_HINT = RBundle.message("interpreter.manager.download.r.hint")

    private fun createInvalidLocationErrorMessage(location: RInterpreterLocation?, details: String?): String {
      val additional = details?.let { ":\n$it" }
      return if (location == null) {
        RBundle.message("interpreter.manager.no.interpreter")
      } else {
        RBundle.message("interpreter.manager.invalid.location", location, additional ?: "")
      }
    }

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
