/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.panel.packageManager

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.r.projectGenerator.template.RProjectSettings
import java.util.*

abstract class RRcppPackageManagerPanel(rProjectSettings: RProjectSettings) : RPackageManagerPanel(rProjectSettings) {

  override val initProjectScriptName: String
    get() = "createRcpp"

  private var rcppSettings = HashMap<String, String>()

  protected fun addRcppSettings(key: String, value: String, isValueFunction: Boolean = false) {
    if (value.isEmpty()) return
    val realValue = if (isValueFunction) {
      value
    }
    else {
      "'$value'"
    }

    rcppSettings[key] = realValue
  }

  protected fun addRcppSettings(key: String, value: Boolean) {
    val realValue = if (value) {
      "TRUE"
    }
    else {
      "FALSE"
    }

    rcppSettings[key] = realValue
  }

  override fun generateProject(project: Project, baseDir: VirtualFile, module: Module) {
    generateRcppProject(project, baseDir)
  }

  protected fun generateRcppProject(project: Project, baseDir: VirtualFile) {

    val parentDirPath = baseDir.parent.path
    val packageDir = baseDir.name
    val rcppSettingsString = StringJoiner(", ", ", ", "")
    rcppSettings.forEach { rcppSettingsString.add("${it.key} = ${it.value}") }
    rcppSettings.clear()

    initializePackage(project, baseDir, listOf(rPackageName, packageDir, parentDirPath, rcppSettingsString.toString()))
    focusFile(project, baseDir, "Read-and-delete-me")
  }
}
