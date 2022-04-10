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
import com.intellij.util.system.CpuArch
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.util.RPathUtil
import org.jetbrains.r.util.tryRegisterDisposable
import java.nio.file.Path

class RFsNotifier(private val interpreter: RInterpreter): Disposable {
  private var processHandler: ProcessHandler? = null
  private val listeners = mutableListOf<Pair<Path, (Path) -> Unit>>()
  private val processLock = Any()

  fun addListener(roots: List<String>, parentDisposable: Disposable, listener: (Path) -> Unit) {
    if (roots.isEmpty()) return
    synchronized(listeners) {
      roots.forEach {
        val path = RPathUtil.toPath(it)
        if (path == null) {
          LOG.error("FsNotifier can't convert '$it' to path")
        } else {
          listeners.add(path to listener)
        }
      }
    }
    updateProcess()
    parentDisposable.tryRegisterDisposable(Disposable {
      synchronized(listeners) {
        roots.forEach {
          val path = RPathUtil.toPath(it) ?: return@forEach
          listeners.remove(path to listener)
        }
      }
      updateProcess()
    })
  }

  override fun dispose() {
    synchronized(processLock) {
      processHandler?.destroyProcess()
      processHandler = null
    }
  }

  private fun updateProcess() {
    runAsync {
      val listenersCopy = synchronized(listeners) { listeners.toList() }
      synchronized(processLock) {
        if (Disposer.isDisposed(this)) return@runAsync
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
            writer.write(it.first.toAbsolutePath().toString())
            writer.newLine()
          }
          writer.write("#")
          writer.newLine()
          writer.flush()
        }
      }
    }
  }

  private fun fireListeners(stringPath: String) = synchronized(listeners) {
    val path = RPathUtil.toPath(stringPath) ?: return
    listeners.forEach { (root, listener) ->
      if (path.startsWith(root)) {
        try {
          listener(path)
        } catch (t: Throwable) {
          LOG.error(t)
        }
      }
    }
  }

  private fun runProcess(): ProcessHandler {
    val executableName = getFsNotifierExecutableName(interpreter.hostOS, interpreter.hostArch) ?: throw RuntimeException("fsnotifier is not supported for ${interpreter.hostOS} ${interpreter.hostArch}")
    val fsNotifierExecutable = RPluginUtil.findFileInRHelpers(executableName)
    if (!fsNotifierExecutable.exists()) {
      throw RuntimeException("fsNotifier: '$executableName' not found in helpers")
    }
    if (!fsNotifierExecutable.canExecute()) fsNotifierExecutable.setExecutable(true)
    val process = interpreter.runProcessOnHost(GeneralCommandLine(interpreter.uploadFileToHost(fsNotifierExecutable)), isSilent = true)

    process.addProcessListener(object : ProcessListener {
      var lastOp: WatcherOp? = null
      val stdoutBuf = StringBuilder()

      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val text = event.text
        when (outputType) {
          ProcessOutputType.STDERR -> LOG.warn("STDERR: $text")
          ProcessOutputType.STDOUT -> {
            text.forEach { c ->
              if (c == '\n') {
                stdoutBuf.toString().trim().takeIf { it.isNotEmpty() }?.let { processLine(it) }
                stdoutBuf.clear()
              } else {
                stdoutBuf.append(c)
              }
            }
          }
        }
      }

      fun processLine(line: String) {
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

    fun getFsNotifierExecutableName(operatingSystem: OperatingSystem, arch: CpuArch) = when (operatingSystem) {
      OperatingSystem.WINDOWS -> "fsnotifier-windows.exe"
      OperatingSystem.LINUX -> when (arch) {
        CpuArch.X86_64 -> "fsnotifier-linux"
        CpuArch.ARM64 -> "fsnotifier-linux-aarch64"
        else -> null
      }
      OperatingSystem.MAC_OS -> "fsnotifier-osx"
    }
  }
}