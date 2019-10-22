/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package icons.org.jetbrains.r.projectGenerator.panel.packageManager

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.r.projectGenerator.panel.packageManager.RPackageManagerPanel
import org.jetbrains.r.projectGenerator.template.RProjectSettings

class RDefaultPackagePanel(rProjectSettings: RProjectSettings) : RPackageManagerPanel(rProjectSettings) {

  override val panelName: String
    get() = "Default package manager panel"

  override val packageManagerName: String
    get() = "Default"

  override val rPackageName: String
    get() = "utils"

  override val initProjectScriptName: String
    get() = "createDefault"

  override val initializingTitle: String
    get() = "Creating default R Package"

  override val initializingIndicatorText: String
    get() = "Initializing..."

  override fun generateProject(project: Project, baseDir: VirtualFile, module: Module) {
    val parentDirPath = baseDir.parent.path
    val packageDir = baseDir.name

    initializePackage(project, baseDir, listOf(packageDir, parentDirPath))
    focusFile(project, baseDir, "Read-and-delete-me")
  }
}