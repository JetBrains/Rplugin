// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbServiceImpl
import com.intellij.openapi.project.Project
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
import org.jetbrains.r.rinterop.RCondaUtil
import org.jetbrains.r.settings.RInterpreterSettings
import java.io.File
import java.io.InputStream
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit

object RInterpreterUtil {
  val DEFAULT_TIMEOUT
    get() = Registry.intValue("r.interpreter.defaultTimeout", 2 * 60 * 1000)
  val RWRAPPER_INITIALIZED_TIMEOUT
    get() = Registry.intValue("r.interpreter.initializedTimeout", 20 * 1000)
  private val R_DISTRO_REGEX = "R-.*".toRegex()
  private const val RPLUGIN_OUTPUT_BEGIN = ">>>RPLUGIN>>>"
  private const val RPLUGIN_OUTPUT_END = "<<<RPLUGIN<<<"
  private val SUGGESTED_INTERPRETER_NAME = RBundle.message("project.settings.suggested.interpreter")

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

  fun tryGetVersionByPath(interpreterPath: String): Version? {
    return try {
      getVersionByPath(interpreterPath)
    } catch (_: Exception) {
      null
    }
  }

  fun getVersionByPath(interpreterPath: String): Version? {
    fun parseVersion(line: String?): Version? {
      return if (line?.startsWith("R version") == true) {
        val items = line.split(' ')
        Version.parseVersion(items[2])
      } else {
        null
      }
    }

    fun checkOutput(inputStream: InputStream): Version? {
      return inputStream.bufferedReader().use {
        val line = it.readLine()
        parseVersion(line)
      }
    }
    if (SystemInfo.isWindows) {
      val result = runRInterpreter(interpreterPath, listOf(interpreterPath, "--version"), null)
      return parseVersion(result.stdoutLines.firstOrNull()) ?: parseVersion(result.stderrLines.firstOrNull())
    }
    // that's fine just to run R normally on Unix because it's a script
    val commandLine = GeneralCommandLine().withExePath(interpreterPath).withParameters("--version")
    val process = commandLine.createProcess()
    return if (process.waitFor(RWRAPPER_INITIALIZED_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)) {
      checkOutput(process.inputStream) ?: checkOutput(process.errorStream)
    } else {
      null
    }
  }

  fun isSupportedVersion(version: Version?): Boolean {
    return version != null && version.isOrGreaterThan(3, 3) && version.lessThan(3, 7)
  }

  fun suggestAllInterpreters(enabledOnly: Boolean): List<RInterpreterInfo> {
    fun MutableList<RInterpreterInfo>.addInterpreter(path: String, name: String) {
      if (findByPath(path) == null) {
        RBasicInterpreterInfo.from(name, path)?.let { inflated ->
          add(inflated)
          RInterpreterSettings.addOrEnableInterpreter(inflated)
        }
      }
    }

    fun suggestAllExisting(): List<RInterpreterInfo> {
      return mutableListOf<RInterpreterInfo>().apply {
        addAll(RInterpreterSettings.existingInterpreters)
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
      val disabledPaths = RInterpreterSettings.disabledPaths
      existing.filter { it.interpreterPath !in disabledPaths }
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
   * @throws RuntimeException if errorHandler is null and the helper exited with non-zero code or produced zero length output.
   */
  fun runHelper(interpreterPath: String,
                helper: File,
                workingDirectory: String?,
                args: List<String>,
                errorHandler: ((ProcessOutput) -> Unit)? = null): String {
    val scriptName = helper.name
    val time = System.currentTimeMillis()
    try {
      val result = runAsync { runHelperWithArgs(interpreterPath, helper, workingDirectory, args) }
                     .onError { RInterpreterImpl.LOG.error(it) }
                     .blockingGet(DEFAULT_TIMEOUT) ?: throw RuntimeException("Timeout for helper '$scriptName'")
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
        RInterpreterImpl.LOG.warn("Helper '$scriptName' has non-blank stderr:\n${result.stderr}")
      }
      if (result.stdout.isBlank()) {
        if (errorHandler != null) {
          errorHandler(result)
          return ""
        }
        throw RuntimeException("Cannot get any output from helper '$scriptName'")
      }
      return getScriptStdout(result.stdout)
    } finally {
      RInterpreterImpl.LOG.warn("Running ${scriptName} took ${System.currentTimeMillis() - time}ms")
    }
  }

  fun createProcessHandlerForHelper(
    interpreterPath: String,
    helper: File,
    workingDirectory: String?,
    args: List<String>
  ): CapturingProcessHandler {
    val commands = getRunHelperCommands(interpreterPath, helper, args)
    return createProcessHandler(interpreterPath, commands, workingDirectory)
  }

  fun createProcessHandler(interpreterPath: String, commands: List<String>, workingDirectory: String?): CapturingProcessHandler {
    val interpreterFile = Paths.get(interpreterPath).toFile()
    val conda = RCondaUtil.findCondaByRInterpreter(interpreterFile)
    val command = if (conda != null) {
      val environment = RCondaUtil.getEnvironmentName(interpreterFile)
      if (environment == null) {
        mutableListOf(conda.absolutePath, "run").apply { addAll(commands) }
      } else {
        mutableListOf(conda.absolutePath, "run", "-n", environment).apply { addAll(commands) }
      }
    } else {
      commands
    }
    return CapturingProcessHandler(GeneralCommandLine(command).withWorkDirectory(workingDirectory))
  }

  fun getScriptStdout(lines: String): String {
    val start = lines.indexOf(RPLUGIN_OUTPUT_BEGIN).takeIf { it != -1 }
                ?: throw RuntimeException("Cannot find start marker, output '$lines'")
    val end = lines.indexOf(RPLUGIN_OUTPUT_END).takeIf { it != -1 }
              ?: throw RuntimeException("Cannot find end marker, output '$lines'")
    return lines.substring(start + RPLUGIN_OUTPUT_BEGIN.length, end)
  }

  private fun runHelperWithArgs(interpreterPath: String, helper: File, workingDirectory: String?, args: List<String>): ProcessOutput {
    val commands = getRunHelperCommands(interpreterPath, helper, args)
    return runRInterpreter(interpreterPath, commands, workingDirectory)
  }

  private fun getRunHelperCommands(interpreterPath: String, helper: File, args: List<String>): List<String> {
    return mutableListOf(interpreterPath, "--slave", "-f", helper.absolutePath, "--args").apply { addAll(args) }
  }

  private fun runRInterpreter(interpreterPath: String,
                              defaultCommands: List<String>,
                              workingDirectory: String?): ProcessOutput {
    val processHandler = createProcessHandler(interpreterPath, defaultCommands, workingDirectory)
    val output = processHandler.runProcess(DEFAULT_TIMEOUT)
    if (output.exitCode != 0) {
      RInterpreterImpl.LOG.warn("Failed to run script. Exit code: " + output.exitCode)
      RInterpreterImpl.LOG.warn(output.stderr)
    }
    return output
  }

  private data class CondaPath(val path: String, val environment: String)
}
