// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbServiceImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.Version
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.EnvironmentUtil
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexImpl
import com.intellij.util.indexing.UnindexedFilesUpdater
import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.configuration.RSettingsProjectConfigurable
import org.jetbrains.r.lexer.SingleStringTokenLexer
import org.jetbrains.r.notifications.RNotificationUtil
import org.jetbrains.r.rinterop.RCondaUtil
import org.jetbrains.r.settings.RInterpreterSettings
import java.io.File
import java.nio.file.Paths
import java.util.*

interface RMultiOutputProcessor {
  fun beforeStart()
  fun onOutputAvailable(output: String)
  fun onTerminated(exitCode: Int, stderr: String)
}

object RInterpreterUtil {
  val DEFAULT_TIMEOUT
    get() = Registry.intValue("r.interpreter.defaultTimeout", 2 * 60 * 1000)
  val RWRAPPER_INITIALIZED_TIMEOUT
    get() = Registry.intValue("r.interpreter.initializedTimeout", 20 * 1000)
  private val R_DISTRO_REGEX = "R-.*".toRegex()
  private const val RPLUGIN_OUTPUT_BEGIN = ">>>RPLUGIN>>>"
  private const val RPLUGIN_OUTPUT_END = "<<<RPLUGIN<<<"
  private val SUGGESTED_INTERPRETER_NAME = RBundle.message("project.settings.suggested.interpreter")
  private val GO_TO_SETTINGS_HINT = RBundle.message("interpreter.manager.go.to.settings.hint")
  private val DOWNLOAD_R_HINT = RBundle.message("interpreter.manager.download.r.hint")

  private val fromPathVariable: ArrayList<String>
    get() {
      val result = ArrayList<String>()

      val path = System.getenv("PATH")
      for (pathEntry in StringUtil.split(path, ";")) {
        if (pathEntry.length < 2) continue

        val noQuotes = removeQuotes(pathEntry)
        val f = File(noQuotes, "R.exe")
        if (f.exists()) {
          result.add(FileUtil.toSystemDependentName(f.path))
        }
      }

      return result
    }

  private fun removeQuotes(pathEntry: String): String? {
    return if (pathEntry.first() == '"' && pathEntry.last() == '"') pathEntry.substring(1, pathEntry.length - 1) else pathEntry
  }

  private fun parseVersion(line: String?): Version? {
    return if (line?.startsWith("R version") == true) {
      val items = line.split(' ')
      items.getOrNull(2)?.let { Version.parseVersion(it) }
    } else {
      null
    }
  }

  fun getVersionByPath(path: String): Version? {
    return getVersionByLocation(RLocalInterpreterLocation(path))
  }

  fun getVersionByLocation(interpreterLocation: RInterpreterLocation): Version? {
    val result = runRInterpreter(interpreterLocation, listOf("--version"), null)
    val version = parseVersion(result.stdoutLines.firstOrNull()) ?: parseVersion(result.stderrLines.firstOrNull())
    if (version != null) return version

    val script = RPluginUtil.findFileInRHelpers("R/GetVersion.R").takeIf { it.exists() } ?: return null
    val output = runHelper(interpreterLocation, script, null, emptyList())
    return parseVersion(output.lineSequence().firstOrNull())
  }

  fun isSupportedVersion(version: Version?): Boolean {
    return version != null && version.isOrGreaterThan(3, 4) && version.lessThan(4, 2)
  }

  fun suggestAllInterpreters(enabledOnly: Boolean, localOnly: Boolean = false): List<RInterpreterInfo> {
    fun MutableList<RInterpreterInfo>.addInterpreter(path: String, name: String) {
      if (findByPath(path) == null) {
        RBasicInterpreterInfo.from(name, RLocalInterpreterLocation(path))?.let { inflated ->
          add(inflated)
          RInterpreterSettings.addOrEnableInterpreter(inflated)
        }
      }
    }

    fun suggestAllExisting(): List<RInterpreterInfo> {
      return mutableListOf<RInterpreterInfo>().apply {
        if (localOnly) {
          addAll(RInterpreterSettings.existingInterpreters.filter { it.interpreterLocation is RLocalInterpreterLocation })
        } else {
          addAll(RInterpreterSettings.existingInterpreters)
        }
        suggestAllHomePaths().forEach { path ->
          addInterpreter(path, SUGGESTED_INTERPRETER_NAME)
        }
        suggestCondaPaths().forEach { (path, environment) ->
          addInterpreter(path, environment)
        }
      }
    }

    val existing = suggestAllExisting()
    return if (enabledOnly) {
      val disabled = RInterpreterSettings.disabledLocations
      existing.filter { it.interpreterLocation.toString() !in disabled }
    } else {
      existing
    }
  }

  fun suggestHomePath(): String {
    return suggestAllHomePaths().firstOrNull() ?: ""
  }

  private fun suggestCondaPaths(): List<CondaPath> {
    val result = mutableListOf<CondaPath>()
    val systemCondaExecutable = RCondaUtil.getSystemCondaExecutable() ?: return result
    val condaRoot = RCondaUtil.getCondaRoot(systemCondaExecutable)?.absolutePath ?: return result
    val rbin = if (SystemInfo.isWindows) "R.exe" else "R"
    Paths.get(condaRoot, "bin", rbin).takeIf { it.exists() }?.let {
      result.add(CondaPath(it.toString(), "(base)"))
    }
    Paths.get(condaRoot, "envs").takeIf { it.exists() && it.isDirectory() }
                                        ?.toFile()?.listFiles()
                                        ?.filter { it.isDirectory }
                                        ?.forEach { env ->
      Paths.get(env.absolutePath, "bin", rbin).takeIf { it.exists() }?.let {
        result.add(CondaPath(it.toString(), env.name))
      }
    }
    return result
  }

  fun suggestAllHomePaths(): List<String> {
    if (ApplicationManager.getApplication().isUnitTestMode && EnvironmentUtil.getValue("RPLUGIN_INTERPRETER_PATH") != null) {
      return listOf(EnvironmentUtil.getValue("RPLUGIN_INTERPRETER_PATH")!!)
    }
    return suggestHomePaths()
  }

  private fun suggestHomePaths(): List<String> {
    if (SystemInfo.isWindows) {
      return fromPathVariable.union(suggestHomePathInProgramFiles()).toList()
    }
    try {
      val rFromPath = CapturingProcessHandler(GeneralCommandLine("which", "R"))
        .runProcess(DEFAULT_TIMEOUT).stdout.trim { it <= ' ' }

      if (rFromPath.isNotEmpty()) {
        return listOf(rFromPath)
      }
    }
    catch (e: ExecutionException) {
      e.printStackTrace()
    }

    if (SystemInfo.isMac) {
      val macosPathOptions = listOf("/Library/Frameworks/R.framework/Resources/bin/R", "/usr/local/bin/R")
      return filterPathOptions(macosPathOptions)
    }
    else if (SystemInfo.isUnix) {
      val linuxPathOptions = listOf("/usr/bin/R")
      return filterPathOptions(linuxPathOptions)
    }
    return ArrayList()
  }

  private fun filterPathOptions(pathOptions: List<String>): List<String> {
    return pathOptions.filter { path -> File(path).isFile && File(path).canExecute() }
  }

  private fun suggestHomePathInProgramFiles(): List<String> =
    listOfNotNull(EnvironmentUtil.getValue("ProgramFiles"), EnvironmentUtil.getValue("ProgramFiles(x86)"))
      .flatMap {
        Paths.get(it, "R").toFile().takeIf { rDir -> rDir.exists() && rDir.isDirectory }
          ?.listFiles { _, name -> name.matches(R_DISTRO_REGEX) }
          ?.mapNotNull { rDistro ->
            Paths.get(rDistro.absolutePath, "bin", "R.exe").toFile().takeIf { exe -> exe.exists() }?.absolutePath
          }
        ?: emptyList()
      }

  fun updateIndexableSet(project: Project) {
    val dumbService = DumbServiceImpl.getInstance(project)
    if (FileBasedIndex.getInstance() is FileBasedIndexImpl) {
      dumbService.queueTask(UnindexedFilesUpdater(project))
    }
  }

  /**
   * @param errorHandler if errorHelper is not null, it could be called instead of throwing the exception.
   * @param workingDirectory try not pass `null` if you are not sure that the execution is the same for all possible working directories
   * @throws RuntimeException if errorHandler is null and the helper exited with non-zero code or produced zero length output.
   */
  fun runHelper(interpreterLocation: RInterpreterLocation,
                helper: File,
                workingDirectory: String?,
                args: List<String>,
                errorHandler: ((ProcessOutput) -> Unit)? = null): String {
    val scriptName = helper.name
    val result = runHelperBase(interpreterLocation, helper, workingDirectory, args) { CapturingProcessAdapter(it) }
    if (result.exitCode != 0) {
      if (errorHandler != null) {
        errorHandler(result)
        return ""
      } else {
        val message = "Helper '$scriptName' has non-zero exit code: ${result.exitCode}"
        throw RuntimeException("$message\nStdout: ${result.stdout}\nStderr: ${result.stderr}")
      }
    }
    if (result.stderr.isNotBlank()) {
      // Note: this could be a warning message therefore there is no need to throw
      RInterpreterBase.LOG.warn("Helper '$scriptName' has non-blank stderr:\n${result.stderr}")
    }
    if (result.stdout.isBlank()) {
      if (errorHandler != null) {
        errorHandler(result)
        return ""
      }
      throw RuntimeException("Cannot get any output from helper '$scriptName'")
    }
    return getScriptStdout(result.stdout)
  }

  /**
   * @see [runHelper]
   */
  fun runMultiOutputHelper(interpreterLocation: RInterpreterLocation,
                           helper: File,
                           workingDirectory: String?,
                           args: List<String>,
                           processor: RMultiOutputProcessor) {
    runHelperBase(interpreterLocation, helper, workingDirectory, args) { RMultiOutputProcessAdapter.createProcessAdapter(it, processor) }
  }

  private fun runHelperBase(interpreterLocation: RInterpreterLocation,
                            helper: File,
                            workingDirectory: String?,
                            args: List<String>,
                            processAdapterProducer: (ProcessOutput) -> ProcessAdapter): ProcessOutput {
    val scriptName = helper.name
    val time = System.currentTimeMillis()
    try {
      return runAsync { runHelperWithArgs(interpreterLocation, helper, workingDirectory, args, processAdapterProducer) }
               .onError { RInterpreterBase.LOG.error(it) }
               .blockingGet(DEFAULT_TIMEOUT) ?: throw RuntimeException("Timeout for helper '$scriptName'")
    }
    finally {
      RInterpreterBase.LOG.warn("Running ${scriptName} took ${System.currentTimeMillis() - time}ms")
    }
  }

  private fun getScriptStdout(lines: String): String {
    val start = lines.indexOf(RPLUGIN_OUTPUT_BEGIN).takeIf { it != -1 }
                ?: throw RuntimeException("Cannot find start marker, output '$lines'")
    val end = lines.indexOf(RPLUGIN_OUTPUT_END).takeIf { it != -1 }
              ?: throw RuntimeException("Cannot find end marker, output '$lines'")
    return lines.substring(start + RPLUGIN_OUTPUT_BEGIN.length, end)
  }

  private fun runHelperWithArgs(interpreterLocation: RInterpreterLocation,
                                helper: File,
                                workingDirectory: String?,
                                args: List<String>,
                                processAdapterProducer: (ProcessOutput) -> ProcessAdapter): ProcessOutput {
    val helperOnHost = interpreterLocation.uploadFileToHost(helper)
    val interpreterArgs = getRunHelperArgs(helperOnHost, args)
    return runRInterpreter(interpreterLocation, interpreterArgs, workingDirectory, processAdapterProducer)
  }

  fun getRunHelperArgs(helper: String, args: List<String>): List<String> {
    return listOf("--slave", "--no-restore", "-f", helper, "--args", *args.toTypedArray())
  }

  private fun runRInterpreter(interpreterLocation: RInterpreterLocation,
                              args: List<String>,
                              workingDirectory: String?,
                              processAdapterProducer: (ProcessOutput) -> ProcessAdapter = { CapturingProcessAdapter(it) }): ProcessOutput {
    val processHandler = interpreterLocation.runInterpreterOnHost(args, workingDirectory)
    val processRunner = CapturingProcessRunner(processHandler, processAdapterProducer)
    val output = processRunner.runProcess(DEFAULT_TIMEOUT)
    if (output.exitCode != 0) {
      RInterpreterBase.LOG.warn("Failed to run script. Exit code: " + output.exitCode)
      RInterpreterBase.LOG.warn(output.stderr)
    }
    return output
  }

  fun checkInterpreterLocation(project: Project, location: RInterpreterLocation): Boolean {
    val (isViable, e) = try {
      Pair(location.getVersion() != null, null)
    } catch (e: Exception) {
      Pair(false, e)
    }
    if (!isViable) {
      showInvalidLocationErrorMessage(project, location, e?.message)
    }
    return isViable
  }

  fun showInvalidLocationErrorMessage(project: Project, location: RInterpreterLocation, details: String?) {
    val additional = details?.let { ":\n$it" }
    val message = RBundle.message("interpreter.manager.invalid.location", location, additional ?: ".")
    val settingsAction = RNotificationUtil.createNotificationAction(GO_TO_SETTINGS_HINT) {
      ShowSettingsUtil.getInstance().showSettingsDialog(project, RSettingsProjectConfigurable::class.java)
    }
    val downloadAction = RNotificationUtil.createNotificationAction(DOWNLOAD_R_HINT) {
      RInterpreterManagerImpl.openDownloadRPage()
    }
    RNotificationUtil.notifyInterpreterError(project, message, settingsAction, downloadAction)
  }

  // TODO: run via helper
  fun loadInterpreterVersionInfo(interpreterLocation: RInterpreterLocation): Map<String, String> {
    return runScript("version", interpreterLocation)?.stdoutLines?.map { it.split(' ', limit = 2) }
             ?.filter { it.size == 2 }?.map { it[0] to it[1].trim() }?.toMap() ?: emptyMap()
  }

  fun runScript(scriptText: String, interpreterLocation: RInterpreterLocation): ProcessOutput? {
    val args = listOf("--no-restore", "--quiet", "--slave", "-e", scriptText)
    try {
      val processHandler = interpreterLocation.runInterpreterOnHost(args)
      return CapturingProcessRunner(processHandler).runProcess(DEFAULT_TIMEOUT)
    } catch (e: Throwable) {
      RLocalInterpreterImpl.LOG.info("Failed to run R executable: \n" +
                                     "Interpreter " + interpreterLocation + "\n" +
                                     "Exception occurred: " + e.message)
    }
    return null
  }

  private data class CondaPath(val path: String, val environment: String)

  private class RMultiOutputProcessAdapter private constructor(private val output: ProcessOutput,
                                                               private val processor: RMultiOutputProcessor) : ProcessAdapter() {

    private val stdoutBuffer = StringBuffer()
    private val beginLexer = SingleStringTokenLexer(RPLUGIN_OUTPUT_BEGIN, stdoutBuffer)
    private val endLexer = SingleStringTokenLexer(RPLUGIN_OUTPUT_END, stdoutBuffer)
    private var markerOccurred: Boolean = false

    override fun processTerminated(event: ProcessEvent) {
      val exitCode = event.exitCode
      output.exitCode = exitCode
      processor.onTerminated(exitCode, output.stderr)
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
      val text = event.text
      if (outputType == ProcessOutputTypes.STDOUT) {
        for (char in text) {
          processCharacter(char)
        }
      }
      else if (outputType == ProcessOutputTypes.STDERR) {
        output.appendStderr(text)
      }
    }

    private fun processCharacter(char: Char) {
      if (!markerOccurred) {
        if (beginLexer.advanceChar(char)) {
          markerOccurred = true
          beginLexer.restore()
          stdoutBuffer.setLength(0)
        }
      }
      else {
        if (endLexer.advanceChar(char)) {
          markerOccurred = false
          endLexer.restore()
          processor.onOutputAvailable(stdoutBuffer.toString())
          stdoutBuffer.setLength(0)
        }
      }
    }

    companion object {
      /**
       * Not pass stdout to the [output]. Process it using [processor]
       */
      fun createProcessAdapter(output: ProcessOutput, processor: RMultiOutputProcessor): RMultiOutputProcessAdapter {
        processor.beforeStart()
        return RMultiOutputProcessAdapter(output, processor)
      }
    }
  }
}
