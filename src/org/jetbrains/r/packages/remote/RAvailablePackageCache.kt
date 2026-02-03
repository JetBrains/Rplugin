/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "RAvailablePackageCache", storages = [Storage("rAvailablePackageCache.xml")])
class RAvailablePackageCache : RCache<RRepoPackage>, SimplePersistentStateComponent<RAvailablePackageCache.State>(State()) {
  @Volatile
  private var lastPackages: List<RRepoPackage>? = null

  @Volatile
  private var lastUrls: List<String>? = null

  override var values: List<RRepoPackage>
    get() = lastPackages ?: fetchPackages()
    set(newPackages) {
      updatePackages(newPackages)
    }

  var urls: List<String>
    get() = lastUrls ?: fetchUrls()
    set(newUrls) {
      updateUrls(newUrls)
    }

  override val lastUpdate: Long
    get() = state.lastUpdate

  @Synchronized
  private fun fetchPackages(): List<RRepoPackage> {
    return state.packages.map { it.toRepoPackage() }.also { packages ->
      lastPackages = packages
    }
  }

  @Synchronized
  private fun updatePackages(newPackages: List<RRepoPackage>) {
    state.packages = newPackages.asSequence().map { it.toSerializable() }.toMutableList()
    state.lastUpdate = System.currentTimeMillis()
    lastPackages = newPackages.toList()  // Note: make a copy
  }

  @Synchronized
  private fun fetchUrls(): List<String> {
    return state.repoUrls.also { urls ->
      lastUrls = urls
    }
  }

  @Synchronized
  private fun updateUrls(newUrls: List<String>) {
    state.repoUrls = newUrls.toMutableList()
    lastUrls = state.repoUrls
  }

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
