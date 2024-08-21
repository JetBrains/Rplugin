/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.messages.Topic
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.util.tryRegisterDisposable
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.PROJECT)
class RLibraryWatcher(private val project: Project) : Disposable {
  private val switch = RLibraryWatcherSwitch()
  private val changed = AtomicBoolean(false)
  private val changedRoots = AtomicReference<List<String>>(emptyList())
  private val currentRoots: MutableMap<String, MutableSet<RInterpreterState>> = mutableMapOf()
  private var currentDisposable: Disposable? = null
  private val timer = LibraryWatcherTimer(1000)

  @Synchronized
  fun updateRootsToWatch(state: RInterpreterState) {
    val roots = state.libraryPaths.map { it.path }
    var hasNewAppeared = false
    for (root in roots) {
      if (!currentRoots.containsKey(root)) {
        hasNewAppeared = true
        currentRoots[root] = mutableSetOf()
      }
      currentRoots.getValue(root).add(state)
    }
    if (!hasNewAppeared) return

    currentDisposable?.let {
      Disposer.dispose(it)
      currentDisposable = null
    }
    if (currentRoots.isNotEmpty()) {
      val disposable = Disposer.newDisposable()
      currentDisposable = disposable
      state.rInterop.tryRegisterDisposable(disposable)
      val currentsRoots = currentRoots.keys.toList()
      state.rInterop.interpreter.addFsNotifierListenerForHost(currentsRoots, disposable) { path ->
        val newChangedRoots = currentsRoots.filter { path.startsWith(it) }
        changedRoots.getAndUpdate { it + newChangedRoots }
        changed.set(true)
        scheduleRefresh()
      }
    }
  }

  fun stopWatchingForState(state: RInterpreterState) {
    val roots = state.libraryPaths.map { it.path }
    for (root in roots) {
      currentRoots[root]?.remove(state) ?: continue
      if (currentRoots.getValue(root).isEmpty()) {
        currentRoots.remove(root)
      }
    }
  }

  fun scheduleRefresh() = timer.scheduleRefresh()

  private fun refresh() {
    if (changed.compareAndSet(true, false)) {
      val roots = changedRoots.getAndSet(emptyList())
      if (roots.isEmpty()) return
      switch.onActive {
        project.messageBus.syncPublisher(TOPIC).libraryChanged(roots.distinct())
      }
    }
  }

  fun disable() {
    switch.disable()
  }

  fun enable() {
    switch.enable()
  }

  override fun dispose() {
    timer.cancel()
  }

  enum class TimeSlot {
    /** Time slot for services that should be updated at first */
    FIRST,

    /** Time slot for services that should be updated early but depend on services from previous slot */
    SECOND,

    /** Time slot for elements that rely on services updated in previous slots (UI panels, for instance) */
    LAST
  }

  private inner class LibraryWatcherTimer(private val delay: Long) {
    private val threadName = "R Library Watcher refresher"
    private var timer = Timer(threadName)
    private val refreshTask
      get() = object : TimerTask() {
        override fun run() = refresh()
      }

    fun cancel() {
      timer.cancel()
    }

    @Synchronized
    fun scheduleRefresh() {
      timer.cancel()
      timer = Timer(threadName)
      timer.schedule(refreshTask, delay)
    }
  }

  companion object {
    // Note: ::class.java doesn't allow to use lambdas instead of full-weight interface implementations
    private val LOG = Logger.getInstance(RLibraryWatcher::class.java)
    private val TOPIC = Topic("R Library Changes", RLibraryListener::class.java)
    private val LOGGER = Logger.getInstance(RLibraryWatcher::class.java)

    private class SlottedListenerDispatcher(project: Project) {
      private val groups = TimeSlot.entries.map { mutableListOf<(List<String>) -> Promise<Unit>>() }

      init {
        val connection = project.messageBus.connect(getInstance(project))
        connection.subscribe(TOPIC, object : RLibraryListener {
          override fun libraryChanged(changedRoots: List<String>) {
            val copy = groups.map { it.toList() }  // Note: if new listeners are going to be added during refresh they won't be taken into account
            updateAllGroups(copy, changedRoots)
          }
        })
      }

      private fun updateAllGroups(groups: List<List<(List<String>) -> Promise<Unit>>>, changedRoots: List<String>) {
        updateRemainingGroups(groups, 0, changedRoots)
      }

      private fun updateRemainingGroups(groups: List<List<(List<String>) -> Promise<Unit>>>, groupIndex: Int, changedRoots: List<String>) {
        if (groupIndex < groups.size) {
          updateGroup(groups[groupIndex], changedRoots)
            .onSuccess { updateRemainingGroups(groups, groupIndex + 1, changedRoots) }
            .onError { LOGGER.error(it) }
        }
      }

      private fun updateGroup(group: List<(List<String>) -> Promise<Unit>>, changedRoots: List<String>): Promise<Unit> {
        return AsyncPromise<Unit>().also { promise ->
          if (group.isEmpty()) {
            promise.setResult(Unit)
            return@also
          }
          val counter = AtomicInteger(group.size)
          for (listener in group) {
            listener(changedRoots)
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

      fun addListener(timeSlot: TimeSlot, listener: (List<String>) -> Promise<Unit>) {
        groups[timeSlot.ordinal].add(listener)
      }
    }

    private val dispatchers = mutableMapOf<String, SlottedListenerDispatcher>()

    @Synchronized
    fun subscribe(project: Project, timeSlot: TimeSlot, listener: (List<String>) -> Promise<Unit>) {
      val dispatcher = dispatchers.getOrPut(project.name) { SlottedListenerDispatcher(project) }
      dispatcher.addListener(timeSlot, listener)
    }

    @Synchronized
    fun subscribeAsync(project: Project, timeSlot: TimeSlot, listener: (List<String>) -> Unit) {
      subscribe(project, timeSlot) {
        runAsync { listener(it) }
      }
    }

    fun getInstance(project: Project): RLibraryWatcher = project.getService(RLibraryWatcher::class.java)
  }
}

interface RLibraryListener {
  fun libraryChanged(changedRoots: List<String>)
}