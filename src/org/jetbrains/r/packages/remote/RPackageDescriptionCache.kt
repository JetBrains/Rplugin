/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote

import com.intellij.openapi.components.*

// Note: there is no need to save this list independently for each project
// since it's doesn't rely on interpreter version whatsoever
@State(name = "RPackageDescription", storages = [Storage("rPackageDescription.xml")])
class RPackageDescriptionCache : SimplePersistentStateComponent<RPackageDescriptionCache.State>(State()) {
  var descriptions: Map<String, String>
    get() = state.descriptions
    set(value) {
      state.descriptions.apply {
        clear()
        putAll(value)
      }
      state.lastUpdate = System.currentTimeMillis()
    }

  val lastUpdate: Long
    get() = state.lastUpdate

  class State : BaseState() {
    var descriptions: MutableMap<String, String> by map<String, String>()
    var lastUpdate: Long by property(0L)
  }

  companion object {
    fun getInstance() = service<RPackageDescriptionCache>()
  }
}
