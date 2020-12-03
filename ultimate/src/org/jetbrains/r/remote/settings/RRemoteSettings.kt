package org.jetbrains.r.remote.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@State(name = "RRemoteSettings", storages = [Storage("rRemoteSettings.xml")])
class RRemoteSettings(private val project: Project) : SimplePersistentStateComponent<RRemoteSettings.State>(State()) {
  var closeRemoteHostView: Boolean
    get() = state.closeRemoteFiles
    set(value) {
      state.closeRemoteFiles = value
    }

  class State : BaseState() {
    var closeRemoteFiles by property(true)
  }

  companion object {
    fun getInstance(project: Project): RRemoteSettings = project.service()
  }
}
