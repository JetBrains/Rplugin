/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.util.messages.Topic
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
import org.jetbrains.r.RPluginUtil
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class RLibraryWatcher(private val project: Project) : Disposable {
  private val switch = RLibraryWatcherSwitch()
  private var interpreter: RInterpreter? = null
  private var fsNotifierProcess: ProcessHandler? = null
  private val changed = AtomicBoolean(false)

  internal fun setCurrentInterpreter(newInterpreter: RInterpreter?) {
    fsNotifierProcess?.let {
      it.destroyProcess()
      fsNotifierProcess = null
    }
    interpreter = newInterpreter
    if (newInterpreter == null) return
    runBackgroundableTask(RBundle.message("library.watcher.initializing"), project) {
      fsNotifierProcess = runFsNotifier(newInterpreter)
      updateRootsToWatch()
    }
  }

  @Synchronized
  fun updateRootsToWatch() {
    val roots = interpreter?.libraryPaths?.map { it.path } ?: return
    fsNotifierProcess?.processInput?.bufferedWriter()?.let { writer ->
      writer.write("ROOTS")
      writer.newLine()
      roots.forEach {
        writer.write(it)
        writer.newLine()
      }
      writer.write("#")
      writer.newLine()
      writer.flush()
    }
  }

  private enum class WatcherOp { GIVEUP, RESET, UNWATCHEABLE, REMAP, MESSAGE, CREATE, DELETE, STATS, CHANGE, DIRTY, RECDIRTY }

  private fun runFsNotifier(interpreter: RInterpreter): ProcessHandler? {
    val executableName = getFsNotifierExecutableName(interpreter.hostOS)
    val fsNotifierExecutable = RPluginUtil.findFileInRHelpers(executableName)
    if (!fsNotifierExecutable.exists()) {
      LOG.error("fsNotifier: '$executableName' not found in helpers")
      return null
    }
    if (!fsNotifierExecutable.canExecute()) fsNotifierExecutable.setExecutable(true)
    val process = interpreter.runProcessOnHost(GeneralCommandLine(interpreter.uploadFileToHost(fsNotifierExecutable)), isSilent = true)
    Disposer.register(project, Disposable { process.destroyProcess() })

    process.addProcessListener(object : ProcessListener {
      var lastOp: WatcherOp? = null

      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val line = event.text.trim().takeIf { it.isNotEmpty() } ?: return
        if (outputType == ProcessOutputType.STDERR) {
          LOG.warn("fsNotifier: $line")
          return
        }
        if (outputType != ProcessOutputType.STDOUT) return

        when (lastOp) {
          WatcherOp.UNWATCHEABLE -> {
            if (line != "#") {
              LOG.warn("fsNotifier: UNWATCHABLE $line")
              return
            }
          }
          WatcherOp.REMAP -> if (line != "#") return
          WatcherOp.MESSAGE -> {
            LOG.warn("fsNotifier: MESSAGE $line")
          }
          WatcherOp.CREATE, WatcherOp.DELETE, WatcherOp.STATS, WatcherOp.CHANGE -> {
            changed.set(true)
          }
          else -> {
            lastOp = try {
              WatcherOp.valueOf(line)
            } catch (e: IllegalArgumentException) {
              LOG.warn("fsNotifier: unknown command $line")
              null
            }
            if (lastOp == WatcherOp.GIVEUP || lastOp == WatcherOp.RESET) {
              LOG.warn("fsNotifier: $line")
            } else {
              return
            }
          }
        }
        lastOp = null
      }

      override fun processTerminated(event: ProcessEvent) {
      }

      override fun startNotified(event: ProcessEvent) {
      }
    }, project)

    process.startNotify()
    return process
  }

  fun refresh() {
    if (changed.compareAndSet(true, false)) {
      switch.onActive {
        project.messageBus.syncPublisher(TOPIC).libraryChanged()
      }
    }
  }

  fun disable() {
    switch.disable()
  }

  fun enable() {
    switch.enable()
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
    private val LOG = Logger.getInstance(RLibraryWatcher::class.java)
    private val TOPIC = Topic("R Library Changes", RLibraryListener::class.java)
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

    fun getFsNotifierExecutableName(operatingSystem: OperatingSystem) = when (operatingSystem) {
      OperatingSystem.WINDOWS -> "fsnotifier-win.exe"
      OperatingSystem.LINUX -> "fsnotifier-linux"
      OperatingSystem.MAC_OS -> "fsnotifier-osx"
    }
  }
}

interface RLibraryListener {
  fun libraryChanged()
}