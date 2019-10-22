// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.Version
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.EnvironmentUtil
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.notifications.RNotificationUtil
import org.jetbrains.r.settings.RInterpreterSettings
import java.io.File
import java.io.InputStream
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit

object RInterpreterUtil {
  const val DEFAULT_TIMEOUT = 5 * 60 * 1000 // 5 min
  const val EDT_TIMEOUT = 5 * 1000 // 5 sec

  private const val INTERPRETER_GROUP_ID = "RInterpreter"
  private const val INTERPRETER_ALIVE_TIMEOUT = 2000L
  private val R_DISTRO_REGEX = "R-.*".toRegex()

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

  fun notifyError(project: Project, message: String?) {
    RNotificationUtil.notifyError(project, INTERPRETER_GROUP_ID, "R Interpreter Failure", message)
  }

  fun tryGetVersionByPath(interpreterPath: String): Version? {
    return try {
      getVersionByPath(interpreterPath)
    } catch (_: Exception) {
      null
    }
  }

  fun getVersionByPath(interpreterPath: String): Version? {
    fun checkOutput(inputStream: InputStream): Version? {
      return inputStream.bufferedReader().use {
        val line = it.readLine()
        if (line?.startsWith("R version") == true) {
          val items = line.split(' ')
          Version.parseVersion(items[2])
        } else {
          null
        }
      }
    }

    val commandLine = GeneralCommandLine().withExePath(interpreterPath).withParameters("--version")
    val process = commandLine.createProcess()
    return if (process.waitFor(INTERPRETER_ALIVE_TIMEOUT, TimeUnit.MILLISECONDS)) {
      checkOutput(process.inputStream) ?: checkOutput(process.errorStream)
    } else {
      null
    }
  }

  fun isSupportedVersion(version: Version?): Boolean {
    return version != null && version.isOrGreaterThan(3, 3) && version.lessThan(3, 7)
  }

  fun suggestAllInterpreters(enabledOnly: Boolean): List<RInterpreterInfo> {
    fun suggestAllExisting(): List<RInterpreterInfo> {
      return mutableListOf<RInterpreterInfo>().apply {
        addAll(RInterpreterSettings.existingInterpreters)
        val suggestedPaths = suggestAllHomePaths()
        for (path in suggestedPaths) {
          if (findByPath(path) == null) {
            RBasicInterpreterInfo.from(SUGGESTED_INTERPRETER_NAME, path)?.let { inflated ->
              add(inflated)
              RInterpreterSettings.addOrEnableInterpreter(inflated)
            }
          }
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
}
