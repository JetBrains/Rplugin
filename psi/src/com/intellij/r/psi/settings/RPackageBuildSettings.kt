/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class RPackageBuildSettings(project: Project) {
  private val state = loadState(project)

  var mainArchitectureOnly: Boolean
    get() = state.mainArchitectureOnly
    set(value) {
      state.mainArchitectureOnly = value
    }

  var useDevTools: Boolean
    get() = state.useDevTools
    set(value) {
      state.useDevTools = value
    }

  var keepSources: Boolean
    get() = state.keepSources
    set(value) {
      state.keepSources = value
    }

  var cleanBuild: Boolean
    get() = state.cleanBuild
    set(value) {
      state.cleanBuild = value
    }

  var asCran: Boolean
    get() = state.asCran
    set(value) {
      state.asCran = value
    }

  var installArgs: List<String>
    get() = state.installArgs
    set(value) {
      state.installArgs = value.toMutableList()
    }

  var checkArgs: List<String>
    get() = state.checkArgs
    set(value) {
      state.checkArgs = value.toMutableList()
    }

  private fun loadState(project: Project): State {
    val rootState = RSettings.getInstance(project).state
    return rootState.packageBuildSettingsState ?: State().also { state ->
      rootState.packageBuildSettingsState = state
    }
  }

  class State : BaseState() {
    var mainArchitectureOnly by property(true)
    var useDevTools by property(true)
    var keepSources by property(true)
    var cleanBuild by property(true)
    var asCran by property(true)

    var installArgs by list<String>()
    var checkArgs by list<String>()
  }

  companion object {
    fun getInstance(project: Project) = project.service<RPackageBuildSettings>()
  }
}
