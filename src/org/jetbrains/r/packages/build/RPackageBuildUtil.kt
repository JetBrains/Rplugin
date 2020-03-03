/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.build

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import java.io.File
import java.nio.file.Paths

object RPackageBuildUtil {
  private val KEY = Key.create<Unit>("org.jetbrains.r.packages.build.RPackageBuildUtil")

  /**
   * Determine whether this [project] corresponds to the custom R Package project.
   * **Note:** a project is considered a package project if and only if
   * its base directory contains "DESCRIPTION" file
   * (similar to R Studio and "devtools")
   */
  fun isPackage(project: Project): Boolean {
    return hasDescription(project) || isMarkedAsPackage(project)
  }

  /**
   * Mark this [project] as the R Package one.
   * This should be used during the creation of new project
   * in order to make [isPackage] method recognize it correctly when
   * DESCRIPTION file hasn't been created yet
   */
  fun markAsPackage(project: Project) {
    project.putUserData(KEY, Unit)
  }

  fun usesRcpp(project: Project): Boolean {
    return findRcppExports(project) != null
  }

  fun getPackageName(project: Project): String? {
    return project.basePath?.let { File(it).name }
  }

  private fun isMarkedAsPackage(project: Project): Boolean {
    return project.getUserData(KEY) != null
  }

  private fun hasDescription(project: Project): Boolean {
    return findDescription(project) != null
  }

  private fun findDescription(project: Project): File? {
    return findInProject(project, "DESCRIPTION")
  }

  private fun findRcppExports(project: Project): File? {
    return findInProject(project, "R", "RcppExports.R")
  }

  private fun findInProject(project: Project, vararg pathElements: String): File? {
    return project.basePath?.let { basePath ->
      val filePath = Paths.get(basePath, *pathElements)
      filePath.toFile().takeIf { it.exists() }
    }
  }
}
