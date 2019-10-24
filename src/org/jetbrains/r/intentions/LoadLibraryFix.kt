/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.intentions

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.intentions.DependencyManagementFix
import org.jetbrains.r.console.RConsoleRuntimeInfo

class LoadLibraryFix(override val packageName: String,
                     private val runtimeInfo: RConsoleRuntimeInfo) : DependencyManagementFix() {

  override fun getName(): String {
    return RBundle.message("load.library.fix.name", packageName)
  }

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    runtimeInfo.loadPackage(packageName)
  }
}