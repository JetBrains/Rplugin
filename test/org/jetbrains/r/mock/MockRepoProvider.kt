/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.mock

import org.jetbrains.concurrency.Promise
import org.jetbrains.r.packages.remote.RMirror
import org.jetbrains.r.packages.remote.RRepoPackage
import org.jetbrains.r.packages.remote.RRepository
import org.jetbrains.r.packages.remote.RepoProvider

class MockRepoProvider : RepoProvider {
  override var selectedCranMirrorIndex: Int
    get() = throw NotImplementedError()
    set(_) {
      throw NotImplementedError()
    }

  override val cranMirrorsAsync: Promise<List<RMirror>>
    get() = throw NotImplementedError()

  override val mappedEnabledRepositoryUrlsAsync: Promise<List<String>>
    get() = throw NotImplementedError()

  override val repositorySelectionsAsync: Promise<List<Pair<RRepository, Boolean>>>
    get() = throw NotImplementedError()

  override val name2AvailablePackages: Map<String, RRepoPackage>
    get() = throw NotImplementedError()

  override val allPackagesCachedAsync: Promise<List<RRepoPackage>>
    get() = throw NotImplementedError()

  override fun loadAllPackagesAsync(): Promise<List<RRepoPackage>> {
    throw NotImplementedError()
  }

  override fun selectRepositories(repositorySelections: List<Pair<RRepository, Boolean>>) {
    throw NotImplementedError()
  }

  override fun onInterpreterVersionChange() {
    throw NotImplementedError()
  }
}
