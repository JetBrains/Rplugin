/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.mock

import org.jetbrains.r.packages.remote.*

class MockRepoProvider : RepoProvider {
  override var selectedCranMirrorIndex: Int
    get() = throw NotImplementedError()
    set(_) {
      throw NotImplementedError()
    }

  override suspend fun getCranMirrors(): List<RMirror> = throw NotImplementedError()

  override suspend fun getMappedEnabledRepositoryUrls(): List<String> = throw NotImplementedError()

  override suspend fun getRepositorySelections(): List<RRepositoryWithSelection> = throw NotImplementedError()

  override val name2AvailablePackages: Map<String, RRepoPackage>
    get() = throw NotImplementedError()

  override suspend fun getAllPackagesCached(): List<RRepoPackage> = throw NotImplementedError()

  override suspend fun loadAllPackages(): List<RRepoPackage> = throw NotImplementedError()

  override fun selectRepositories(repositorySelections: List<Pair<RRepository, Boolean>>) {
    throw NotImplementedError()
  }

  override fun onInterpreterVersionChange() {
    throw NotImplementedError()
  }
}
