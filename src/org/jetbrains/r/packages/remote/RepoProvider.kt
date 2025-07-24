/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

interface RepoProvider {
  var selectedCranMirrorIndex: Int
  suspend fun getCranMirrors(): List<RMirror>
  suspend fun getMappedEnabledRepositoryUrls(): List<String>
  suspend fun getRepositorySelections(): List<RRepositoryWithSelection>
  val name2AvailablePackages: Map<String, RRepoPackage>?
  suspend fun getAllPackagesCached(): List<RRepoPackage>
  suspend fun loadAllPackages(): List<RRepoPackage>
  fun selectRepositories(repositorySelections: List<Pair<RRepository, Boolean>>)
  fun onInterpreterVersionChange()

  companion object {
    fun getInstance(project: Project) = project.service<RepoProvider>()
  }
}
