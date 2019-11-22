/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.Version
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.PathUtilRt
import icons.org.jetbrains.r.RBundle
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.packages.RHelpersUtil
import org.jetbrains.r.settings.RSettings
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Paths
import java.util.concurrent.TimeoutException

object RInteropUtil {
  val LOG = Logger.getInstance(RInteropUtil.javaClass)
  fun runRWrapperAndInterop(project: Project): Promise<RInterop> {
    val promise = AsyncPromise<RInterop>()
    var createdProcess: OSProcessHandler? = null
    ProcessIOExecutorService.INSTANCE.execute {
      runRWrapper(project).onError {
        promise.setError(it)
      }.onSuccess { process ->
        createdProcess = process
        createRInterop(process, project, promise)
      }
    }
    return promise.onError { createdProcess?.destroyProcess() }
  }

  private fun createRInterop(process: ColoredProcessHandler,
                             project: Project,
                             promise: AsyncPromise<RInterop>) {
    val linePromise = AsyncPromise<String>()
    process.addProcessListener(object : ProcessListener {
      val output = StringBuilder()
      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val text = event.text
        when (outputType) {
          ProcessOutputType.STDERR, ProcessOutputType.SYSTEM -> LOG.debug("RWRAPPER " + StringUtil.escapeStringCharacters(text))
          ProcessOutputType.STDOUT -> {
            if (linePromise.state != Promise.State.PENDING) return
            output.append(text)
            if (text.contains('\n')) linePromise.setResult(output.toString())
          }
        }
      }

      override fun processTerminated(event: ProcessEvent) {
        LOG.info("RWRAPPER TERMINATED, code=${event.exitCode}")
        if (output.isNotBlank()) {
          LOG.info(output.toString())
        }
        val updateCrashes = updateCrashes()
        if (updateCrashes.isNotEmpty()) {
          reportMinidumps(updateCrashes)
        }
        if (linePromise.state == Promise.State.PENDING) linePromise.setError(RuntimeException("RWrapper terminated"))
      }

      override fun startNotified(event: ProcessEvent) {
        LOG.info("RWRAPPER STARTED")
      }

      override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
      }
    })
    process.startNotify()
    ProcessIOExecutorService.INSTANCE.execute {
      try {
        val line = try {
          linePromise.blockingGet(RWRAPPER_LAUNCH_TIMEOUT) ?: ""
        } catch (e: TimeoutException) {
          throw RuntimeException("RWrapper does not produce output")
        }
        val port = Regex("PORT (\\d+)\\n").find(line)?.groupValues?.getOrNull(1)?.toIntOrNull()
                   ?: throw RuntimeException("Invalid RWrapper output")
        val rInterop = RInterop(process, "127.0.0.1", port, project)
        val rScriptsPath = RHelpersUtil.findFileInRHelpers("R").takeIf { it.exists() }?.absolutePath
                           ?: throw RuntimeException("R Scripts not found")
        val projectDir = project.basePath ?: throw RuntimeException("Project dir is null")
        val init = rInterop.init(rScriptsPath, projectDir)
        if (init.stdout.isNotBlank()) {
          LOG.warn(init.stdout)
        }
        if (init.stderr.isNotBlank()) {
          LOG.warn(init.stderr)
        }
        promise.setResult(rInterop)
      } catch (e: Throwable) {
        promise.setError(e)
      }
    }
  }

  private fun reportMinidumps(updateCrashes: List<File>) {
    val attachments = updateCrashes.map { file ->
      try {
        val path = file.getPath()
        FileInputStream(file).use { content ->
          val tempFile = FileUtil.createTempFile("ij-attachment-" + PathUtilRt.getFileName(path) + ".", ".bin", true)
          FileOutputStream(tempFile).use { outputStream -> FileUtil.copy(content, file.length(), outputStream) }
          return@map Attachment(path, tempFile, MINIDUMP_DESCRIPTION).apply { isIncluded = true }
        }
      }
      catch (e: IOException) {
        LOG.warn("Cannot create attachment", e)
        return@map Attachment(file.path, e)
      }
    }.toTypedArray()
    LOG.error("RWrapper terminated with Runtime Error, the crash minidump found: ${updateCrashes[0].name} ", *attachments)
  }

  private fun runRWrapper(project: Project): Promise<ColoredProcessHandler> {
    val result = AsyncPromise<ColoredProcessHandler>()
    val interpreterPath = getInterpreterPath(project)
    val paths = getRPaths(interpreterPath)
    val version = RInterpreterUtil.getVersionByPath(interpreterPath)
                  ?: return result.also { result.setError("Cannot parse R interpreter version") }

    if (!RInterpreterUtil.isSupportedVersion(version)) return result.also { result.setError("Unsupported interpreter version " + version)  }
    val wrapperPath = getWrapperPath(version)
    val rwrapper = File(wrapperPath)
    if (!rwrapper.exists()) return result.also { result.setError("Cannot find suitable RWrapper version in " + wrapperPath) }
    if (!rwrapper.canExecute()) {
      rwrapper.setExecutable(true)
    }
    val crashpadHandler = getCrashpadHandler()

    var command = GeneralCommandLine()
      .withExePath(wrapperPath)
      .withWorkDirectory(project.basePath!!)
      .withParameters("--with-timeout")
      .withEnvironment("R_HOME", paths.home)
      .withEnvironment("R_SHARE_DIR", paths.share)
      .withEnvironment("R_INCLUDE_DIR", paths.include)
      .withEnvironment("R_DOC_DIR", paths.doc)

    if (crashpadHandler.exists()) {
      if (!crashpadHandler.canExecute()) {
        crashpadHandler.setExecutable(true)
      }
      val crashes = getRWrapperCrashesDirectory()
      FileUtil.createDirectory(crashes)
      updateCrashes()
      command = command.withEnvironment("CRASHPAD_HANDLER_PATH", crashpadHandler.absolutePath)
                       .withEnvironment("CRASHPAD_DB_PATH", crashes.absolutePath)
    }

    command = if (SystemInfo.isUnix) {
      command.withEnvironment("LD_LIBRARY_PATH", Paths.get(paths.home, "lib").toString())
    }
    else {
      command.withEnvironment("PATH", Paths.get(paths.home, "bin", "x64").toString() + ";" + System.getenv("PATH"))
    }
    command = command.withEnvironment("R_HELPERS_PATH", RHelpersUtil.helpersPath)
    return result.also { result.setResult(ColoredProcessHandler(command).apply { setShouldDestroyProcessRecursively(true) }) }
  }

  private fun getRWrapperCrashesDirectory() = Paths.get(PathManager.getLogPath(), "rwrapper-crashes").toFile()

  private fun getCrashpadHandler(): File = RHelpersUtil.findFileInRHelpers("crashpad_handler-" + getSystemSuffix())

  private fun getWrapperPath(version: Version): String {
    val filename = "rwrapper-" + getSystemSuffix()
    return if (ApplicationManager.getApplication().isInternal || ApplicationManager.getApplication().isUnitTestMode) {
      RHelpersUtil.findFileInRHelpers(filename).takeIf { it.exists() }?.absolutePath ?: getRWrapperByRVersion(version, filename)
    } else {
      getRWrapperByRVersion(version, filename)
    }
  }

  private fun getRWrapperByRVersion(version: Version, relativePath: String): String =
    Paths.get(RHelpersUtil.findFileInRHelpers("R-${version.major}.${version.minor}").absolutePath, relativePath).toString()

  private fun getSystemSuffix(): String = when {
      SystemInfo.isLinux -> "x64-linux"
      SystemInfo.isMac -> "x64-osx"
      SystemInfo.isWindows -> "x64-windows.exe"
      else -> throw IllegalStateException("Unsupported OS")
    }

  private fun getInterpreterPath(project: Project): String {

    val interpreterPath = if (ApplicationManager.getApplication().isUnitTestMode) RInterpreterUtil.suggestHomePath()
    else RSettings.getInstance(project).interpreterPath

    if (StringUtil.isEmptyOrSpaces(interpreterPath)) {
      throw RuntimeException(RBundle.message("console.runner.interpreter.not.specified"))
    }

    if (SystemInfo.isWindows) {
      val exeFile = File(interpreterPath)
      if (!exeFile.exists()) {
        throw RuntimeException("File ${exeFile.absolutePath}  doesn't exist")
      }
      Paths.get(exeFile.parentFile.absolutePath, "x64", "RTerm.exe").toFile().takeIf { it.exists() }?.let {
        return it.absolutePath
      }
      Paths.get(exeFile.parentFile.absolutePath, "i386", "RTerm.exe").toFile().takeIf { it.exists() }?.let {
        return it.absolutePath
      }
    }

    return interpreterPath
  }

  private data class RPaths(val home: String, val share: String, val include: String, val doc: String)

  private fun getRPaths(interpreter: String): RPaths {
    val script = RHelpersUtil.findFileInRHelpers("R/GetEnvVars.R").takeIf { it.exists() }?.absolutePath
                       ?: throw RuntimeException("GetEnvVars.R not found")
    val paths = CapturingProcessHandler(GeneralCommandLine(interpreter, "--slave", "-f", script))
      .runProcess(1000).stdout.trim().split(File.pathSeparator)
    return RPaths(paths[0], paths[1], paths[2], paths[3])
  }

  @Synchronized
  private fun updateCrashes(): List<File> {
    val crashes = Paths.get(getRWrapperCrashesDirectory().absolutePath, "pending")
                       .toFile().takeIf { it.exists() }
                       ?.listFiles { _, name -> name.endsWith(".dmp") } ?: return emptyList()
    val newCrashes = crashes.filter { !oldCrashes.contains(it.name) }
    oldCrashes.clear()

    crashes.sortBy { -it.lastModified() }
    val names = crashes.filterIndexed { index, file ->
      if (index >= MAX_MINIDUMP_COUNT || System.currentTimeMillis() - file.lastModified() > MINIDUMP_LIFETIME) {
        FileUtil.asyncDelete(file)
        return@filterIndexed false
      }
      return@filterIndexed true
    }.map { it.name }

    oldCrashes.addAll(names)

    return newCrashes
  }

  private val oldCrashes: MutableSet<String> = HashSet()

  private const val MAX_MINIDUMP_COUNT = 20
  private const val MINIDUMP_LIFETIME = 1000 * 60 * 60 * 24 * 7 // one week

  private const val RWRAPPER_LAUNCH_TIMEOUT = 2500

  private const val MINIDUMP_DESCRIPTION: String = """The minidump contains OS and R process state.
They are grabbed from the crashed process into
an in-memory snapshot structure. Since the full
application state is typically too large for capturing
to disk and transmitting to an upstream server, the snapshot
contains a heuristically selected subset of the full state
    
The precise details of what’s captured varies between
operating systems, but generally includes the following:

 - The set of modules (executable, shared libraries) that are
   loaded into the crashing process.
 - An enumeration of the threads running in the crashing process,
   including the register contents and the contents of stack memory
   of each thread.
 - A selection of the OS-related state of the process,
   such as e.g. the command line, environment and so on.
 - A selection of memory potentially referenced from registers and
   from stack.
  """
}
