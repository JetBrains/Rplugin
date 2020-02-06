/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.Promise

interface RepoProvider {
  var selectedCranMirrorIndex: Int
  val cranMirrorsAsync: Promise<List<RMirror>>
  val repositorySelectionsAsync: Promise<List<Pair<RRepository, Boolean>>>
  val names2availablePackages: Map<String, RRepoPackage>?
  val allPackagesCachedAsync: Promise<List<RRepoPackage>>
  fun loadAllPackagesAsync(): Promise<List<RRepoPackage>>
  fun selectRepositories(repositorySelections: List<Pair<RRepository, Boolean>>)
  fun onInterpreterVersionChange()

  companion object {
    fun getInstance(project: Project) = project.service<RepoProvider>()
  }
}
