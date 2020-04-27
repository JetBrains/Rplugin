/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.google.common.annotations.VisibleForTesting
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
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.rejectedPromise
import org.jetbrains.r.RBundle
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.R_3_6
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
    var createdProcess: ColoredProcessHandler? = null
    ProcessIOExecutorService.INSTANCE.execute {
      runRWrapper(project).onError {
        promise.setError(it)
      }.onSuccess { (process, paths) ->
        createdProcess = process
        createRInterop(process, project, promise, paths)
      }
    }
    return promise.onError { createdProcess?.destroyProcess() }
  }

  private fun createRInterop(process: ColoredProcessHandler,
                             project: Project,
                             promise: AsyncPromise<RInterop>,
                             paths: RPaths) {
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
        if (updateCrashes.isNotEmpty() || (event.exitCode != 0 && rInteropForReport != null)) {
          reportCrash(rInteropForReport, updateCrashes)
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
        val rInterop = RInterop(process, "127.0.0.1", port, project)
        rInteropForReport = rInterop
        promise.setResult(rInterop)
      } catch (e: Throwable) {
        promise.setError(e)
      }
    }
  }

  internal fun reportCrash(rInterop: RInterop?, updateCrashes: List<File>) {
    if (ApplicationManager.getApplication().isUnitTestMode || rInterop?.isAlive == false) return
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
    val message = "RWrapper terminated with Runtime Error" +
                  (updateCrashes.firstOrNull()?.let { ", the crash minidump found: $it " } ?: "")
    LOG.error(message, *attachments)
  }

  private fun runRWrapper(project: Project): Promise<Pair<ColoredProcessHandler, RPaths>> {
    val result = AsyncPromise<Pair<ColoredProcessHandler, RPaths>>()
    val interpreterPath = RInterpreterManager.getInstance(project).interpreterPath
    if (StringUtil.isEmptyOrSpaces(interpreterPath)) {
      return rejectedPromise(RuntimeException(RBundle.message("console.runner.interpreter.not.specified")))
    }
    val paths = getRPaths(interpreterPath, project)
    val version = RInterpreterUtil.getVersionByPath(interpreterPath)
                  ?: return result.also { result.setError("Cannot parse R interpreter version") }

    if (!RInterpreterUtil.isSupportedVersion(version)) return result.also { result.setError("Unsupported interpreter version " + version)  }
    val wrapperPath = getWrapperPath(version)
    LOG.info("R version is $version. RWrapper path: $wrapperPath")
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

    command = command.withEnvironment("PATH", paths.path)
    command = if (SystemInfo.isUnix) {
      if (SystemInfo.isMac) {
        // DYLD_FALLBACK_LIBRARY_PATH doesn't work if notarization is enabled, use DYLD_LIBRARY_PATH instead.
        // Right now we notarize only bundled binaries.
        val dyldName = if (RPluginUtil.getPlugin().isBundled) "DYLD_LIBRARY_PATH" else "DYLD_FALLBACK_LIBRARY_PATH"
        command.withEnvironment(dyldName, paths.ldPath.takeIf { it.isNotBlank() } ?: "${paths.home}/lib")
      } else {
        command.withEnvironment("LD_LIBRARY_PATH", paths.ldPath.takeIf { it.isNotBlank() } ?: "${paths.home}/lib")
      }
    } else {
      command.withEnvironment("PATH", Paths.get(paths.home, "bin", "x64").toString() + ";" + paths.path)
    }
    command = command.withEnvironment("R_HELPERS_PATH", RPluginUtil.helpersPath)
    return result.also { result.setResult(ColoredProcessHandler(command).apply { setShouldDestroyProcessRecursively(true) } to paths) }
  }

  private fun getRWrapperCrashesDirectory() = Paths.get(PathManager.getLogPath(), "rwrapper-crashes").toFile()

  private fun getCrashpadHandler(): File = RPluginUtil.findFileInRHelpers("crashpad_handler-" + getSystemSuffix())

  private fun getWrapperPath(version: Version): String {
    val filename = "rwrapper-" + getSystemSuffix()
    val fileByVersion = getRWrapperByRVersion(version, filename)
    return if ((ApplicationManager.getApplication().isInternal || ApplicationManager.getApplication().isUnitTestMode) &&
               !File(fileByVersion).exists()) {
      RPluginUtil.findFileInRHelpers(filename).absolutePath
    } else {
      fileByVersion
    }
  }

  private fun getRWrapperByRVersion(version: Version, relativePath: String): String {
    val wrapperVersion = if (version.isOrGreaterThan(3, 4) && !SystemInfo.isMac) {
      R_3_6
    } else {
      version
    }
    val directory = "R-${wrapperVersion.major}.${wrapperVersion.minor}"
    return Paths.get(RPluginUtil.findFileInRHelpers(directory).absolutePath, relativePath).toString()
  }

  private fun getSystemSuffix(): String = when {
      SystemInfo.isLinux -> "x64-linux"
      SystemInfo.isMac -> "x64-osx"
      SystemInfo.isWindows -> "x64-windows.exe"
      else -> throw IllegalStateException("Unsupported OS")
    }

  private data class RPaths(val home: String,
                            val share: String,
                            val include: String,
                            val doc: String,
                            val path: String,
                            val ldPath: String)

  private fun getRPaths(interpreter: String, project: Project): RPaths {

    val script = RPluginUtil.findFileInRHelpers("R/GetEnvVars.R").takeIf { it.exists() }
                 ?: throw RuntimeException("GetEnvVars.R not found")
    
    val output = RInterpreterUtil.runHelper(interpreter, script, project.basePath, emptyList())
    val paths = output.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    if (paths.size < 5) {
      LOG.error("cannot get rwrapper parameters, output: `$output`")
      throw RuntimeException("Cannot get environment variables for running rwrapper")
    }
    return RPaths(paths[0], paths[1], paths[2], paths[3], paths[4], if (paths.size == 6) paths[5] else "")
  }

  @VisibleForTesting
  @Synchronized
  internal fun updateCrashes(): List<File> {
    val directoryWithDumps = if (SystemInfo.isWindows) "reports" else "pending"
    val crashes = Paths.get(getRWrapperCrashesDirectory().absolutePath, directoryWithDumps)
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
