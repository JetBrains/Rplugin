/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.intentions

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.r.RBundle
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageInstaller

class InstallLibrariesFix(private val missing: List<RequiredPackage>) : DependencyManagementFix() {
  override fun getName(): String {
    val packageNamesString = missing.joinToString { it.toFormat(true) }
    return RBundle.message("install.libraries.fix.name", packageNamesString)
  }

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    RequiredPackageInstaller.getInstance(project).installPackagesWithUserPermission(getName(), missing, false)
      .onError { showErrorNotification(project, it) }
  }
}
