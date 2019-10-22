/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.ProjectTopics
import com.intellij.openapi.application.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.DumbServiceImpl
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.psi.impl.PsiDocumentManagerImpl
import com.intellij.psi.search.FileTypeIndex
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexImpl
import com.intellij.util.indexing.UnindexedFilesUpdater
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.RFileType
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.packages.RSkeletonUtil
import org.jetbrains.r.rmarkdown.RMarkdownFileType
import org.jetbrains.r.settings.RInterpreterSettings
import org.jetbrains.r.settings.RSettings

interface RInterpreterManager {
  val interpreter: RInterpreter?

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
  private var interpreterPath = fetchInterpreterPath()
  private var asyncPromise = AsyncPromise<Unit>()
  private var initialized = false
  private var rInterpreter: RInterpreterImpl? = null
  private val mergingUpdateQueue = MergingUpdateQueue("Update Skeletons", 500,
                                                      true, null, project)

  init {
    val connection = project.messageBus.connect()
    connection.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, object : FileEditorManagerListener.Before {
      override fun beforeFileOpened(source: FileEditorManager, file: VirtualFile) {
        if (file.fileType == RFileType || file.fileType == RMarkdownFileType) {
          RConsoleManager.getInstance(project).runIfEmpty()
        }
      }
    })
    connection.subscribe(ProjectTopics.MODULES, object: ModuleListener {
      override fun moduleAdded(project: Project, module: Module) {
        ReadAction.nonBlocking {
          if (FileTypeIndex.containsFileOfType(RFileType, module.moduleContentScope) ||
              FileTypeIndex.containsFileOfType(RMarkdownFileType, module.moduleContentScope)) {
            initializeInterpreter()
          }
        }.inSmartMode(project).submit(AppExecutorUtil.getAppExecutorService())
      }
    })
    val newRFileListener = object: BulkFileListener {
      override fun after(events: MutableList<out VFileEvent>) {
        if (events.any { event -> (event is VFileCopyEvent || event is VFileCreateEvent || event is VFileMoveEvent) &&
                                  (event.file?.fileType == RFileType) } ) {
          initializeInterpreter()
        }
      }
    }
    connection.subscribe(VirtualFileManager.VFS_CHANGES, newRFileListener)
  }

  private fun fetchInterpreterPath(oldPath: String = ""): String {
    return if (ApplicationManager.getApplication().isUnitTestMode) {
      ""
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
      val details = if (e != null) ":\n${e.message}" else ""
      val message = "Unable to use \"$path\" as an R interpreter$details.\nSpecify path to a viable executable"
      RInterpreterUtil.notifyError(project, message)
    }
    return isViable
  }

  private fun ensureActiveInterpreterStored() {
    fun findAmong(existing: List<RInterpreterInfo>): RInterpreterInfo? {
      return existing.find { it.interpreterPath == interpreterPath }
    }

    val known = RInterpreterSettings.knownInterpreters
    if (findAmong(known) == null) {
      rInterpreter?.let { suggested ->
        val interpreter = RBasicInterpreterInfo("Suggested", suggested.interpreterPath, suggested.version)
        RInterpreterSettings.addInterpreter(interpreter)
      }
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
        RLibraryWatcher.subscribe(project, RLibraryWatcher.TimeSlot.EARLY, object : RLibraryListener {
          override fun libraryChanged() {
            scheduleSkeletonUpdate()
          }
        })
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
      }
      updateIndexableSet()
    }
  }

  private fun updateIndexableSet() {
    val dumbService = DumbServiceImpl.getInstance(project)
    if (FileBasedIndex.getInstance() is FileBasedIndexImpl) {
      dumbService.queueTask(UnindexedFilesUpdater(project))
    }
  }

  private fun scheduleSkeletonUpdate() {
    rInterpreter?.updateState()
    mergingUpdateQueue.queue(object: Update("updating skeletons") {
      override fun run() {
        val interpreter = rInterpreter ?: return
        val updater = object : Task.Backgroundable(project, "Update skeletons", false) {
          override fun run(indicator: ProgressIndicator) {
            RLibraryWatcher.getInstance(project).registerRootsToWatch(interpreter.libraryPaths)
            RLibraryWatcher.getInstance(project).refresh()
            if (RSkeletonUtil.updateSkeletons(interpreter)) {
              updateIndexableSet()
              runInEdt { runWriteAction { refreshSkeletons(interpreter) } }
            }
          }
        }
        ProgressManager.getInstance().run(updater)
      }
    })
  }

  private fun Task.Backgroundable.refreshSkeletons(interpreter: RInterpreterImpl) {
    interpreter.skeletonPaths.forEach { skeletonPath ->
      val libraryRoot = LocalFileSystem.getInstance().refreshAndFindFileByPath(skeletonPath) ?: return
      VfsUtil.markDirtyAndRefresh(false, true, true, libraryRoot)
      WriteAction.runAndWait<Exception> { PsiDocumentManagerImpl.getInstance(project).commitAllDocuments() }
    }
  }
}


