/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote

import com.intellij.openapi.components.*

// Note: there is no need to save this list independently for each project
// since it's doesn't rely on interpreter version whatsoever
@State(name = "RPackageDescription", storages = [Storage("rPackageDescription.xml")])
class RPackageDescriptionCache : SimplePersistentStateComponent<RPackageDescriptionCache.State>(State()) {
  @Volatile
  private var lastDescriptions: Map<String, String>? = null

  var descriptions: Map<String, String>
    get() = lastDescriptions ?: fetchDescriptions()
    set(newDescriptions) {
      updateDescriptions(newDescriptions)
    }

  val lastUpdate: Long
    get() = state.lastUpdate

  @Synchronized
  private fun fetchDescriptions(): Map<String, String> {
    return state.descriptions.also { descriptions ->
      lastDescriptions = descriptions
    }
  }

  @Synchronized
  private fun updateDescriptions(newDescriptions: Map<String, String>) {
    state.descriptions = newDescriptions.toMutableMap()
    state.lastUpdate = System.currentTimeMillis()
    lastDescriptions = state.descriptions
  }

  class State : BaseState() {
    var descriptions: MutableMap<String, String> by map()
    var lastUpdate: Long by property(0L)
  }

  companion object {
    fun getInstance() = service<RPackageDescriptionCache>()
  }
}
