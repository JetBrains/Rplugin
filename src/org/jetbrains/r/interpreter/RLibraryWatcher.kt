/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.application.subscribe
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.Topic

class RLibraryWatcher(private val project: Project) {
  private val bulkFileListener: BulkFileListener
  private val rootsToWatch = HashSet<LocalFileSystem.WatchRequest>()
  private val files4Watching = ArrayList<VirtualFile>()
  private val libraryPaths = ArrayList<VirtualFile>()

  init {
    bulkFileListener = object: BulkFileListener {
      override fun after(events: MutableList<out VFileEvent>) {
        if (events.any { event -> event.file?.let { file ->
              libraryPaths.any { ancestor -> VfsUtil.isAncestor(ancestor, file, true) } } == true }) {
            project.messageBus.syncPublisher(TOPIC).libraryChanged()
          }
        }
    }
    VirtualFileManager.VFS_CHANGES.subscribe(project, bulkFileListener)
  }

  @Synchronized
  fun registerRootsToWatch(libraryRoots: List<VirtualFile>) {
    libraryPaths.clear()
    libraryPaths.addAll(libraryRoots)
    val localFileSystem = LocalFileSystem.getInstance()
    localFileSystem.removeWatchedRoots(rootsToWatch)
    rootsToWatch.clear()
    refresh()
    rootsToWatch.addAll(localFileSystem.addRootsToWatch(libraryRoots.map { it.path }, true))
  }

  /**
   * the method shouldn't be called from read action
   */
  @Synchronized
  fun refresh() {
    files4Watching.clear()
    libraryPaths.filter { it.isDirectory }.forEach { root ->
      root.refresh(false, false)
      val libraries = VfsUtil.getChildren(root)
      for (library in libraries.filter { it.isDirectory }) {
        if (!library.isValid) {
          continue
        }
        if (!library.isValid) {
          continue
        }
        val relativeFile = VfsUtil.findRelativeFile(library, "R")
        if (relativeFile != null && relativeFile.isDirectory) {
          files4Watching.addAll(VfsUtil.getChildren(relativeFile))
        }
        files4Watching.add(library)
      }
    }
  }

  enum class TimeSlot {
    /** Time slot for services that should be updated at first */
    EARLY,

    /** Time slot for elements that rely on services updated in early slot (UI panels, for instance) */
    LATE
  }

  companion object {
    // Note: ::class.java doesn't allow to use lambdas instead of full-weight interface implementations
    private val TOPIC = Topic<RLibraryListener>("R Library Changes", RLibraryListener::class.java)

    private class SlottedListenerDispatcher(project: Project) {
      private val groups = TimeSlot.values().map { mutableListOf<RLibraryListener>() }

      init {
        val connection = project.messageBus.connect()
        connection.subscribe(TOPIC, object : RLibraryListener {
          override fun libraryChanged() {
            for (group in groups) {
              for (listener in group) {
                listener.libraryChanged()
              }
            }
          }
        })
      }

      fun addListener(timeSlot: TimeSlot, listener: RLibraryListener) {
        groups[timeSlot.ordinal].add(listener)
      }
    }

    private val dispatchers = mutableMapOf<String, SlottedListenerDispatcher>()

    @Synchronized
    fun subscribe(project: Project, timeSlot: TimeSlot, listener: RLibraryListener) {
      val dispatcher = dispatchers.getOrPut(project.name) { SlottedListenerDispatcher(project) }
      dispatcher.addListener(timeSlot, listener)
    }

    fun getInstance(project: Project): RLibraryWatcher = ServiceManager.getService(project, RLibraryWatcher::class.java)
  }
}

interface RLibraryListener {
  fun libraryChanged()
}