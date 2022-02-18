/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.diagnostic.IdeMessagePanel
import com.intellij.diagnostic.MessagePool
import com.intellij.diagnostic.MessagePoolListener
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.impl.status.FatalErrorWidgetFactory
import com.intellij.util.PathUtilRt
import com.intellij.util.system.CpuArch
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.interpreter.*
import org.jetbrains.r.settings.RSettings
import org.jetbrains.r.util.RPathUtil
import org.jvnet.winp.WinProcess
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeoutException

object RInteropUtil {
  val LOG = Logger.getInstance(RInteropUtil.javaClass)
  fun runRWrapperAndInterop(interpreter: RInterpreter, workingDirectory: String = interpreter.basePath): Promise<RInterop> {
    val promise = AsyncPromise<RInterop>()
    var createdProcess: ProcessHandler? = null
    ProcessIOExecutorService.INSTANCE.execute {
      runRWrapper(interpreter, workingDirectory).onError {
        promise.setError(it)
      }.onSuccess { (process, paths) ->
        createdProcess = process
        createRInterop(process, promise, paths, interpreter)
      }
    }
    return promise.onError { createdProcess?.destroyProcess() }
  }

  private fun createRInterop(process: ProcessHandler,
                             promise: AsyncPromise<RInterop>,
                             paths: RPaths,
                             interpreter: RInterpreter) {
    val linePromise = AsyncPromise<String>()
    var rInteropForReport: RInterop? = null
    val stdout = StringBuilder()
    val stderr = StringBuffer()

    fun generateErrorReport(): String =
      """
      rpath: ${paths}${System.lineSeparator()}
      stdout: ${stdout}
      stderr: ${stderr}
      """

    process.addProcessListener(object : ProcessListener {
      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val text = event.text
        when (outputType) {
          ProcessOutputType.STDERR, ProcessOutputType.SYSTEM -> {
            if (linePromise.state == Promise.State.PENDING) {
              stderr.append(text)
            }
            LOG.debug("RWRAPPER " + StringUtil.escapeStringCharacters(text))
          }
          ProcessOutputType.STDOUT -> {
            if (linePromise.state != Promise.State.PENDING) return
            stdout.append(text)
            if (text.contains('\n')) linePromise.setResult(stdout.toString())
          }
        }
      }

      override fun processTerminated(event: ProcessEvent) {
        LOG.info("RWRAPPER TERMINATED, code=${event.exitCode}")
        if (stdout.isNotBlank()) {
          LOG.info(stdout.toString())
        }
        val updateCrashes = updateCrashes()
        if (updateCrashes.isNotEmpty() || event.exitCode != 0) {
          reportCrash(interpreter.project, rInteropForReport, updateCrashes, process.getUserData(PROCESS_CRASH_REPORT_FILE),
                      process.getUserData(PROCESS_TERMINATED_WITH_REPORT) ?: false)
        }
        if (linePromise.state == Promise.State.PENDING) {
          linePromise.setError(RuntimeException(
            """RWrapper terminated, exitcode: ${event.exitCode}${System.lineSeparator()}
                                ${generateErrorReport()}
                                """.trimIndent()))
        }
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
          linePromise.blockingGet(RInterpreterUtil.RWRAPPER_INITIALIZED_TIMEOUT) ?: ""
        } catch (e: TimeoutException) {
          throw RuntimeException("""RWrapper does not produce output
          ${generateErrorReport()}
          """.trimMargin())
        }
        val port = Regex("PORT (\\d+)\\n").find(line)?.groupValues?.getOrNull(1)?.toIntOrNull()
                   ?: throw RuntimeException("Invalid RWrapper output")
        val rInterop = interpreter.createRInteropForProcess(process, port)
        rInteropForReport = rInterop
        promise.setResult(rInterop)
      } catch (e: Throwable) {
        promise.setError(e)
      }
    }
  }

  private fun reportCrash(project: Project, rInterop: RInterop?, updateCrashes: List<File>,
                          crashReportFile: String? = null, terminatedWithReport: Boolean = false) {
    if (ApplicationManager.getApplication().isUnitTestMode) return
    if (rInterop?.killedByUsed == true) return
    var attachments = updateCrashes.map { file ->
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
    rInterop?.rInteropGrpcLogger?.let {
      val grpcLog = it.toJson(true)
      attachments = attachments.plus(Attachment("grpc_log.json", grpcLog).apply { isIncluded = true })
    }
    val interpreter = rInterop?.interpreter
    if (crashReportFile != null && interpreter != null) {
      val file = if (interpreter.isLocal()) {
        File(crashReportFile)
      } else {
        FileUtil.createTempFile("rwrapper-crash-report", ".txt", true).also {
          interpreter.downloadFileFromHost(crashReportFile, it.absolutePath)
        }
      }
      if (file.exists() && file.isFile) {
        val content = file.readText(Charsets.UTF_8)
        file.delete()
        if (content.isNotEmpty()) {
          attachments = attachments.plus(Attachment("rwrapper-crash-report.txt", content).apply { isIncluded = true })
        }
      }
    }
    val message = (if (terminatedWithReport) "RWrapper forcibly terminated by user" else "RWrapper terminated with Runtime Error") +
                  (updateCrashes.firstOrNull()?.let { ", the crash minidump found: $it " } ?: "")
    if (terminatedWithReport) {
      MessagePool.getInstance().addListener(object : MessagePoolListener {
        override fun newEntryAdded() {
          MessagePool.getInstance().removeListener(this)
          invokeLater {
            (FatalErrorWidgetFactory().createWidget(project) as? IdeMessagePanel)?.openErrorsDialog(null)
          }
        }
      })
    }
    LOG.error(message, *attachments)
  }

  private fun runRWrapper(interpreter: RInterpreter, workingDirectory: String): Promise<Pair<ProcessHandler, RPaths>> {
    val result = AsyncPromise<Pair<ProcessHandler, RPaths>>()
    try {
      val paths = getRPaths(interpreter)
      val version = interpreter.version
      if (!RInterpreterUtil.isSupportedVersion(version)) return result.also {
        result.setError("Unsupported interpreter version " + version)
      }
      val wrapperPath = getWrapperPath(interpreter.hostOS)
      LOG.info("R version is $version. RWrapper path: $wrapperPath")
      val rwrapper = File(wrapperPath)
      if (interpreter.isLocal()) {
        if (!rwrapper.exists()) return result.also { result.setError("Cannot find suitable RWrapper version in " + wrapperPath) }
        if (!rwrapper.canExecute()) rwrapper.setExecutable(true)
      }
      val wrapperPathOnHost = interpreter.uploadFileToHost(rwrapper)
      var command = GeneralCommandLine()
        .withExePath(wrapperPathOnHost)
        .withParameters("--with-timeout")
        .withEnvironment("R_HOME", paths.home)
        .withEnvironment("R_SHARE_DIR", paths.share)
        .withEnvironment("R_INCLUDE_DIR", paths.include)
        .withEnvironment("R_DOC_DIR", paths.doc)

      if (RSettings.getInstance(interpreter.project).disableRprofile) {
        command = command.withParameters("--disable-rprofile")
      }

      if (!interpreter.isLocal()) {
        command = command.withParameters("--is-remote")
      }

      if (interpreter.isLocal()) {
        val crashpadHandler = getCrashpadHandler(interpreter.hostOS)
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
      }
      val crashReportFile = interpreter.createTempFileOnHost("rwrapper-crash-report.txt")
      command = command.withParameters("--crash-report-file", crashReportFile)

      command = command.withEnvironment("PATH", paths.path)
      command = when (interpreter.hostOS) {
        OperatingSystem.MAC_OS -> {
          val dyldName = "DYLD_LIBRARY_PATH"
          command.withEnvironment(dyldName, paths.ldPath.takeIf { it.isNotBlank() } ?: "${paths.home}/lib")
        }
        OperatingSystem.LINUX -> {
          command.withEnvironment("LD_LIBRARY_PATH", paths.ldPath.takeIf { it.isNotBlank() } ?: "${paths.home}/lib")
        }
        OperatingSystem.WINDOWS -> {
          command.withEnvironment("PATH", RPathUtil.join(paths.home, "bin", "x64") + ";" + paths.path)
        }
      }
      result.setResult(interpreter.runProcessOnHost(command, workingDirectory, true).apply {
        this.putUserData(PROCESS_CRASH_REPORT_FILE, crashReportFile)
      } to paths)
    } catch (t: Throwable) {
      result.setError(t)
    }
    return result
  }

  private fun getRWrapperCrashesDirectory() = File(RPathUtil.join(PathManager.getLogPath(), "rwrapper-crashes"))

  private fun getCrashpadHandler(operatingSystem: OperatingSystem): File {
    return RPluginUtil.findFileInRHelpers("crashpad_handler-" + getSystemSuffix(operatingSystem))
  }

  fun getWrapperPath(operatingSystem: OperatingSystem): String {
    val filename = "rwrapper-" + getSystemSuffix(operatingSystem)
    return RPluginUtil.findFileInRHelpers(filename).absolutePath
  }

  fun getSystemSuffix(operatingSystem: OperatingSystem): String = when (operatingSystem) {
    OperatingSystem.LINUX -> "x64-linux"
    OperatingSystem.MAC_OS -> if (CpuArch.isArm64()) "arm64-osx" else "x64-osx"
    OperatingSystem.WINDOWS -> "x64-windows.exe"
  }

  data class RPaths(val home: String,
                    val share: String,
                    val include: String,
                    val doc: String,
                    val path: String,
                    val ldPath: String)

  private fun getRPaths(interpreter: RInterpreter): RPaths {
    val script = RPluginUtil.findFileInRHelpers("R/GetEnvVars.R").takeIf { it.exists() }
                 ?: throw RuntimeException("GetEnvVars.R not found, plugin directory: ${PathManager.getPluginsPath()}")
    val output = interpreter.runHelper(script, emptyList())
    val paths = output.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    if (paths.size < 5) {
      LOG.error("cannot get rwrapper parameters, output: `$output`")
      throw RuntimeException("Cannot get environment variables for running rwrapper")
    }
    return RPaths(paths[0], paths[1], paths[2], paths[3], paths[4], if (paths.size == 6) paths[5] else "")
  }

  @Synchronized
  private fun updateCrashes(): List<File> {
    val directoryWithDumps = if (SystemInfo.isWindows) "reports" else "pending"
    val crashes = File(RPathUtil.join(getRWrapperCrashesDirectory().absolutePath, directoryWithDumps))
                    .takeIf { it.exists() }
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

  fun createRInteropForLocalProcess(interpreter: RInterpreter, processHandler: ProcessHandler, port: Int): RInterop {
    val project = interpreter.project
    val rInterop = RInterop(interpreter, processHandler, "127.0.0.1", port, project)
    val workspaceFile = if (ApplicationManager.getApplication().isUnitTestMode) {
      project.getUserData(WORKSPACE_FILE_FOR_TESTS)
    } else {
      val filename = interpreter.interpreterLocation.hashCode().toString()
      RPathUtil.join(interpreter.basePath, ".RDataFiles", "$filename.RData")
    }
    val rScriptsPath = RPluginUtil.findFileInRHelpers("R").takeIf { it.exists() }?.absolutePath
                       ?: throw RuntimeException("R Scripts not found")
    val projectDir = project.basePath ?: throw RuntimeException("Project dir is null")
    rInterop.init(rScriptsPath, projectDir, workspaceFile)
    rInterop.putUserData(TERMINATE_WITH_REPORT_HANDLER) {
      val process = (processHandler as? OSProcessHandler)?.process ?: return@putUserData
      if (SystemInfo.isWindows) {
        WinProcess(process).sendCtrlC()
      } else {
        UnixProcessManager.sendSignal(OSProcessUtil.getProcessID(process), UnixProcessManager.SIGABRT)
      }
    }
    return rInterop
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

  private val PROCESS_CRASH_REPORT_FILE = Key<String>("org.jetbrains.r.rinterop.RInteropUtil.crashReportFile")
  val PROCESS_TERMINATED_WITH_REPORT = Key<Boolean>("org.jetbrains.r.rinterop.RInteropUtil.terminatedWithReport")
  val WORKSPACE_FILE_FOR_TESTS = Key<String>("org.jetbrains.r.rinterop.RInteropUtil.workspaceFileForTests")
  val TERMINATE_WITH_REPORT_HANDLER = Key<() -> Unit>("org.jetbrains.r.rinterop.RInteropUtil.terminateWithReportHandler")
}
