/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote

import com.intellij.openapi.components.*

// Note: looks like list of available CRAN mirrors doesn't depend on interpreter version
@State(name = "RMirrorCache", storages = [Storage("rMirrorCache.xml")])
class RMirrorCache : RCache<RMirror>, SimplePersistentStateComponent<RMirrorCache.State>(State()) {
  override var values: List<RMirror>
    get() = state.mirrors.map { it.toMirror() }
    set(newMirrors) {
      state.apply {
        mirrors.clear()
        mirrors.addAll(newMirrors.asSequence().map { it.toSerializable() })
        lastUpdate = System.currentTimeMillis()
      }
    }

  override val lastUpdate: Long
    get() = state.lastUpdate

  class RSerializableMirror {
    var name: String = ""
    var url: String = ""
  }

  class State : BaseState() {
    var mirrors by list<RSerializableMirror>()
    var lastUpdate by property(0L)
  }

  companion object {
    private fun RMirror.toSerializable() = RSerializableMirror().also {
      it.name = name
      it.url = url
    }

    private fun RSerializableMirror.toMirror() = RMirror(name, url)

    fun getInstance() = service<RMirrorCache>()
  }
}