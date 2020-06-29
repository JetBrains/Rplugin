/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.application.subscribe
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.Topic
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import java.util.concurrent.atomic.AtomicInteger

class RLibraryWatcher(private val project: Project) : Disposable {
  private val bulkFileListener: BulkFileListener
  private val rootsToWatch = HashSet<LocalFileSystem.WatchRequest>()
  private val files4Watching = ArrayList<VirtualFile>()
  private val libraryPaths = ArrayList<VirtualFile>()
  private val switch = RLibraryWatcherSwitch()

  init {
    bulkFileListener = object: BulkFileListener {
      override fun after(events: List<VFileEvent>) {
        if (events.any { it.isLibraryEvent() }) {
          onLibraryChanged()
        }
      }
    }
    VirtualFileManager.VFS_CHANGES.subscribe(project, bulkFileListener)
  }

  private fun VFileEvent.isLibraryEvent(): Boolean {
    return file?.isLibraryFile() == true
  }

  private fun VirtualFile.isLibraryFile(): Boolean {
    return libraryPaths.any { ancestor -> VfsUtil.isAncestor(ancestor, this, true) }
  }

  private fun onLibraryChanged() {
    switch.onActive {
      project.messageBus.syncPublisher(TOPIC).libraryChanged()
    }
  }

  fun disable() {
    switch.disable()
  }

  fun enable() {
    switch.enable()
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

  override fun dispose() {}

  enum class TimeSlot {
    /** Time slot for services that should be updated at first */
    FIRST,

    /** Time slot for services that should be updated early but depend on services from previous slot */
    SECOND,

    /** Time slot for elements that rely on services updated in previous slots (UI panels, for instance) */
    LAST
  }

  companion object {
    // Note: ::class.java doesn't allow to use lambdas instead of full-weight interface implementations
    private val TOPIC = Topic<RLibraryListener>("R Library Changes", RLibraryListener::class.java)
    private val LOGGER = Logger.getInstance(RLibraryWatcher::class.java)

    private class SlottedListenerDispatcher(project: Project) {
      private val groups = TimeSlot.values().map { mutableListOf<() -> Promise<Unit>>() }

      init {
        val connection = project.messageBus.connect(getInstance(project))
        connection.subscribe(TOPIC, object : RLibraryListener {
          override fun libraryChanged() {
            val copy = groups.map { it.toList() }  // Note: if new listeners are going to be added during refresh they won't be taken into account
            updateAllGroups(copy)
          }
        })
      }

      private fun updateAllGroups(groups: List<List<() -> Promise<Unit>>>) {
        updateRemainingGroups(groups, 0)
      }

      private fun updateRemainingGroups(groups: List<List<() -> Promise<Unit>>>, groupIndex: Int) {
        if (groupIndex < groups.size) {
          updateGroup(groups[groupIndex])
            .onSuccess { updateRemainingGroups(groups, groupIndex + 1) }
            .onError { LOGGER.error(it) }
        }
      }

      private fun updateGroup(group: List<() -> Promise<Unit>>): Promise<Unit> {
        return AsyncPromise<Unit>().also { promise ->
          if (group.isEmpty()) {
            promise.setResult(Unit)
            return@also
          }
          val counter = AtomicInteger(group.size)
          for (listener in group) {
            listener()
              .onSuccess { decreaseCounter(counter, promise) }
              .onError {
                LOGGER.error("Error occurred when triggering RLibraryWatcher listener", it)
                decreaseCounter(counter, promise)
              }
          }
        }
      }

      private fun decreaseCounter(counter: AtomicInteger, promise: AsyncPromise<Unit>) {
        val current = counter.decrementAndGet()
        if (current == 0) {
          promise.setResult(Unit)
        }
      }

      fun addListener(timeSlot: TimeSlot, listener: () -> Promise<Unit>) {
        groups[timeSlot.ordinal].add(listener)
      }
    }

    private val dispatchers = mutableMapOf<String, SlottedListenerDispatcher>()

    @Synchronized
    fun subscribe(project: Project, timeSlot: TimeSlot, listener: () -> Promise<Unit>) {
      val dispatcher = dispatchers.getOrPut(project.name) { SlottedListenerDispatcher(project) }
      dispatcher.addListener(timeSlot, listener)
    }

    @Synchronized
    fun subscribeAsync(project: Project, timeSlot: TimeSlot, listener: () -> Unit) {
      subscribe(project, timeSlot) {
        runAsync(listener)
      }
    }

    fun getInstance(project: Project): RLibraryWatcher = ServiceManager.getService(project, RLibraryWatcher::class.java)
  }
}

interface RLibraryListener {
  fun libraryChanged()
}