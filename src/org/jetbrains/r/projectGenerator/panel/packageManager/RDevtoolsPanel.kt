/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.panel.packageManager

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.r.projectGenerator.template.RProjectSettings

class RDevtoolsPanel(rProjectSettings: RProjectSettings) : RPackageManagerPanel(rProjectSettings) {
  override val panelName: String
    get() = "R package using devtools panel"

  override val packageManagerName: String
    get() = "Devtools"

  override val rPackageName: String
    get() = "devtools"

  override val initProjectScriptName: String
    get() = "createDevtools"

  override fun generateProject(project: Project, baseDir: VirtualFile, module: Module) {
    initializePackage(project, baseDir, listOf(baseDir.path))
    focusFile(project, baseDir, "DESCRIPTION")
  }
}