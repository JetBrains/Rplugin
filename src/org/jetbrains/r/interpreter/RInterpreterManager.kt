/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.impl.PsiDocumentManagerImpl
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.RBundle
import org.jetbrains.r.RFileType
import org.jetbrains.r.configuration.RActiveInterpreterProjectConfigurable
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.notifications.RNotificationUtil
import org.jetbrains.r.packages.RSkeletonUtil
import org.jetbrains.r.rmarkdown.RMarkdownFileType
import org.jetbrains.r.settings.RInterpreterSettings
import org.jetbrains.r.settings.RSettings
import org.jetbrains.r.statistics.RStatistics

interface RInterpreterManager {
  val interpreter: RInterpreter?
  val interpreterPath: String

  fun hasInterpreter(): Boolean {
    return interpreter != null
  }

  fun initializeInterpreter(force: Boolean = false): Promise<Unit>

  companion object {
    fun getInstance(project: Project): RInterpreterManager = project.getComponent(RInterpreterManager::class.java)
    fun getInterpreter(project: Project): RInterpreter? = getInstance(project).interpreter
  }
}

class RInterpreterManagerImpl(private val project: Project): RInterpreterManager {
  @Volatile
  override var interpreterPath: String = fetchInterpreterPath()
    private set
  private var asyncPromise = AsyncPromise<Unit>()
  private var initialized = false
  private var rInterpreter: RInterpreterImpl? = null
  private var firstOpenedFile = true

  init {
    val connection = project.messageBus.connect()
    connection.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, object : FileEditorManagerListener.Before {
      override fun beforeFileOpened(source: FileEditorManager, file: VirtualFile) {
        if (file.fileType == RFileType || file.fileType == RMarkdownFileType) {
          val toolWindowManager = ToolWindowManager.getInstance(project)
          if (firstOpenedFile) {
            firstOpenedFile = false
            toolWindowManager.invokeLater(Runnable {
              toolWindowManager.getToolWindow(RConsoleToolWindowFactory.ID)?.show { }
            })
          } else {
            toolWindowManager.invokeLater(Runnable {
              if (toolWindowManager.getToolWindow(RConsoleToolWindowFactory.ID) != null) {
                RConsoleManager.getInstance(project).currentConsoleAsync
              }
            })
          }
        }
      }
    })
  }

  private fun fetchInterpreterPath(oldPath: String = ""): String {
    return if (ApplicationManager.getApplication().isUnitTestMode) {
      RInterpreterUtil.suggestHomePath()
    } else {
      val settings = RSettings.getInstance(project)
      val suggestedPath = settings.interpreterPath
      if (checkInterpreterPath(suggestedPath)) {
        suggestedPath
      } else {
        settings.interpreterPath = oldPath
        oldPath
      }
    }
  }

  private fun checkInterpreterPath(path: String): Boolean {
    val (isViable, e) = try {
      Pair(path.isNotBlank() && RInterpreterUtil.getVersionByPath(path) != null, null)
    } catch (e: Exception) {
      Pair(false, e)
    }
    if (!isViable) {
      val message = createInvalidPathErrorMessage(path, e?.message)
      val action = RNotificationUtil.createNotificationAction(GO_TO_SETTINGS_HINT) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, RActiveInterpreterProjectConfigurable::class.java)
      }
      RNotificationUtil.notifyInterpreterError(project, message, action)
    }
    return isViable
  }

  private fun ensureActiveInterpreterStored() {
    rInterpreter?.let { suggested ->
      val interpreter = RBasicInterpreterInfo(SUGGESTED_INTERPRETER_NAME, suggested.interpreterPath, suggested.version)
      RInterpreterSettings.addOrEnableInterpreter(interpreter)
    }
  }

  override fun initializeInterpreter(force: Boolean): Promise<Unit> {
    if (initialized && !force) {
      return asyncPromise
    }
    synchronized(this) {
      if (initialized && !force) {
        return asyncPromise
      }
      if (force) {
        rInterpreter = null
        asyncPromise = AsyncPromise()
        interpreterPath = fetchInterpreterPath(interpreterPath)
      }
      if (!initialized) {
        RLibraryWatcher.subscribe(project, RLibraryWatcher.TimeSlot.FIRST) {
          scheduleSkeletonUpdate()
        }
      }
      initialized = true
      if (interpreterPath != "") {
        setupInterpreter(asyncPromise)
        return asyncPromise
      }
    }
    asyncPromise.setError("Cannot initialize interpreter")
    return asyncPromise
  }

  override val interpreter: RInterpreter?
    get() = rInterpreter

  private fun setupInterpreter(promise: AsyncPromise<Unit>) {
    runBackgroundableTask("Initializing R interpreter", project) {
      val versionInfo = RInterpreterImpl.loadInterpreterVersionInfo(interpreterPath, project.basePath!!)
      RInterpreterImpl(versionInfo, interpreterPath, project).let {
        rInterpreter = it
        ensureActiveInterpreterStored()
        scheduleSkeletonUpdate()
        promise.setResult(Unit)
        RStatistics.logSetupInterpreter(it)
      }
    }
  }

  private fun scheduleSkeletonUpdate(): Promise<Unit> {
    return AsyncPromise<Unit>().also { promise ->
      val interpreter = rInterpreter
      if (interpreter != null) {
        interpreter.updateState()
          .onProcessed { promise.setResult(Unit) }
          .onSuccess {
            val updater = object : Task.Backgroundable(project, "Update skeletons", false) {
              override fun run(indicator: ProgressIndicator) {
                RLibraryWatcher.getInstance(project).registerRootsToWatch(interpreter.libraryPaths)
                RLibraryWatcher.getInstance(project).refresh()
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

  private fun updateSkeletons(interpreter: RInterpreterImpl) {
    val dumbModeTask = object : DumbModeTask(interpreter) {
      override fun performInDumbMode(indicator: ProgressIndicator) {
        if (!project.isOpen || project.isDisposed) return
        if (RSkeletonUtil.updateSkeletons(interpreter, project, indicator)) {
          runInEdt { runWriteAction { refreshSkeletons(interpreter) } }
        }
        RInterpreterUtil.updateIndexableSet(project)
      }
    }
    DumbService.getInstance(project).queueTask(dumbModeTask)
  }

  private fun refreshSkeletons(interpreter: RInterpreterImpl) {
    if (!project.isOpen || project.isDisposed) return
    interpreter.skeletonPaths.forEach { skeletonPath ->
      val libraryRoot = LocalFileSystem.getInstance().refreshAndFindFileByPath(skeletonPath) ?: return@forEach
      VfsUtil.markDirtyAndRefresh(false, true, true, libraryRoot)
      WriteAction.runAndWait<Exception> { PsiDocumentManagerImpl.getInstance(project).commitAllDocuments() }
    }
  }

  companion object {
    private val SUGGESTED_INTERPRETER_NAME = RBundle.message("project.settings.suggested.interpreter")
    private val GO_TO_SETTINGS_HINT = RBundle.message("interpreter.manager.go.to.settings.hint")

    private fun createInvalidPathErrorMessage(path: String, details: String?): String {
      val additional = details?.let { ":\n$it" }
      return RBundle.message("interpreter.manager.invalid.path", path, additional ?: "")
    }
  }
}

