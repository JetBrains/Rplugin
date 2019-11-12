/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@State(name = "RAvailablePackageCache", storages = [Storage("rAvailablePackageCache.xml")])
class RAvailablePackageCache : RCache<RRepoPackage>, SimplePersistentStateComponent<RAvailablePackageCache.State>(State()) {
  override var values: List<RRepoPackage>
    get() {
      val chunked = state.flattenPackages.chunked(ENTRIES_PER_PACKAGE)
      return chunked.map { it.toRepoPackage() }
    }
    set(packages) {
      state.flattenPackages.apply {
        clear()
        addAll(packages.flatMap { it.toFlattenPackage() })
      }
      state.lastUpdate = System.currentTimeMillis()
    }

  override val lastUpdate: Long
    get() = state.lastUpdate

  class State : BaseState() {
    var flattenPackages: MutableList<String> by list<String>()
    var lastUpdate: Long by property(0L)
  }

  companion object {
    private const val ENTRIES_PER_PACKAGE = 4

    private fun RRepoPackage.toFlattenPackage(): List<String> {
      return listOf(name, repoUrl ?: "", latestVersion ?: "", depends ?: "")  // Actually 'repoUrl' and 'latestVersion' will never be null
    }

    private fun List<String>.toRepoPackage(): RRepoPackage {
      val depends = this[3].let { if (it.isNotEmpty()) it else null }
      return RRepoPackage(this[0], this[1], this[2], depends)
    }

    fun getInstance(project: Project) = project.service<RAvailablePackageCache>()
  }
}
