package org.jetbrains.r.interpreter

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.util.tryRegisterDisposable

class RFsNotifier(private val interpreter: RInterpreter) {
  private var processHandler: ProcessHandler? = null
  private val listeners = mutableListOf<Pair<String, (String) -> Unit>>()
  private val processLock = Any()

  init {
    Disposer.register(interpreter.project, Disposable { processHandler?.destroyProcess() })
  }

  fun addListener(roots: List<String>, parentDisposable: Disposable, listener: (String) -> Unit) {
    if (roots.isEmpty()) return
    synchronized(listeners) {
      roots.forEach { listeners.add(it to listener) }
    }
    updateProcess()
    parentDisposable.tryRegisterDisposable(Disposable {
      synchronized(listeners) {
        roots.forEach { listeners.remove(it to listener) }
      }
      updateProcess()
    })
  }

  private fun updateProcess() {
    runAsync {
      val listenersCopy = synchronized(listeners) { listeners.toList() }
      synchronized(processLock) {
        if (listenersCopy.isEmpty()) {
          processHandler?.destroyProcess()
          processHandler = null
          return@runAsync
        }
        val process = processHandler ?: runProcess().also { processHandler = it }
        process.processInput?.bufferedWriter()?.let { writer ->
          writer.write("ROOTS")
          writer.newLine()
          listenersCopy.forEach {
            writer.write(it.first)
            writer.newLine()
          }
          writer.write("#")
          writer.newLine()
          writer.flush()
        }
      }
    }
  }

  private fun fireListeners(path: String) = synchronized(listeners) {
    val pathFixed = fixPath(path)
    listeners.forEach { (root, listener) ->
      if (pathFixed.startsWith(fixPath(root))) {
        try {
          listener(path)
        } catch (t: Throwable) {
          LOG.error(t)
        }
      }
    }
  }

  private fun fixPath(path: String): String {
    return if (interpreter.hostOS == OperatingSystem.WINDOWS) path.replace('\\', '/') else path
  }

  private fun runProcess(): ProcessHandler {
    val executableName = getFsNotifierExecutableName(interpreter.hostOS)
    val fsNotifierExecutable = RPluginUtil.findFileInRHelpers(executableName)
    if (!fsNotifierExecutable.exists()) {
      throw RuntimeException("fsNotifier: '$executableName' not found in helpers")
    }
    if (!fsNotifierExecutable.canExecute()) fsNotifierExecutable.setExecutable(true)
    val process = interpreter.runProcessOnHost(GeneralCommandLine(interpreter.uploadFileToHost(fsNotifierExecutable)), isSilent = true)

    process.addProcessListener(object : ProcessListener {
      var lastOp: WatcherOp? = null

      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val line = event.text.trim().takeIf { it.isNotEmpty() } ?: return
        if (outputType == ProcessOutputType.STDERR) {
          LOG.warn("STDERR: $line")
          return
        }
        if (outputType != ProcessOutputType.STDOUT) return

        when (lastOp) {
          WatcherOp.UNWATCHEABLE -> {
            if (line != "#") {
              LOG.warn("UNWATCHABLE: $line")
              return
            }
          }
          WatcherOp.REMAP -> if (line != "#") return
          WatcherOp.MESSAGE -> {
            LOG.warn("MESSAGE: $line")
          }
          WatcherOp.CREATE, WatcherOp.DELETE, WatcherOp.STATS, WatcherOp.CHANGE, WatcherOp.DIRTY, WatcherOp.RECDIRTY -> {
            fireListeners(line)
          }
          else -> {
            lastOp = try {
              WatcherOp.valueOf(line)
            } catch (e: IllegalArgumentException) {
              LOG.warn("unknown command $line")
              null
            }
            if (lastOp == WatcherOp.GIVEUP || lastOp == WatcherOp.RESET) {
              LOG.warn(line)
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
    })

    process.startNotify()
    return process
  }

  private enum class WatcherOp { GIVEUP, RESET, UNWATCHEABLE, REMAP, MESSAGE, CREATE, DELETE, STATS, CHANGE, DIRTY, RECDIRTY }

  companion object {
    private val LOG = Logger.getInstance(RLibraryWatcher::class.java)

    fun getFsNotifierExecutableName(operatingSystem: OperatingSystem) = when (operatingSystem) {
      OperatingSystem.WINDOWS -> "fsnotifier-win.exe"
      OperatingSystem.LINUX -> "fsnotifier-linux"
      OperatingSystem.MAC_OS -> "fsnotifier-osx"
    }
  }
}