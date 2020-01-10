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
      return state.packages.map { it.toRepoPackage() }
    }
    set(newPackages) {
      state.packages.apply {
        clear()
        addAll(newPackages.map { it.toSerializable() })
      }
      state.lastUpdate = System.currentTimeMillis()
    }

  var urls: List<String>
    get() = state.repoUrls
    set(newUrls) {
      state.repoUrls.apply {
        clear()
        addAll(newUrls)
      }
    }

  override val lastUpdate: Long
    get() = state.lastUpdate

  class SerializablePackage {
    var name = ""
    var repoUrl = ""
    var latestVersion = ""
    var depends = ""
  }

  class State : BaseState() {
    var repoUrls by list<String>()
    var packages by list<SerializablePackage>()
    var lastUpdate by property(0L)
  }

  companion object {
    private fun RRepoPackage.toSerializable(): SerializablePackage {
      return SerializablePackage().also {
        it.name = name
        it.repoUrl = repoUrl ?: ""  // Note: will never be null
        it.latestVersion = latestVersion ?: ""  // Note: will never be null
        it.depends = depends ?: ""
      }
    }

    private fun SerializablePackage.toRepoPackage(): RRepoPackage {
      return RRepoPackage(name, repoUrl, latestVersion, depends.takeIf { it.isNotBlank() })
    }

    fun getInstance(project: Project) = project.service<RAvailablePackageCache>()
  }
}
