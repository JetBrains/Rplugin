// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.interpreter.RInterpreterUtil

@State(name = "RSettings", storages = [Storage("rSettings.xml")])
class RSettings(private val project: Project) : SimplePersistentStateComponent<RSettings.State>(State()) {
  var interpreterPath: String
    get() = state.interpreterPath ?: fetchInterpreterPath()
    set(value) {
      state.interpreterPath = value
    }

  var loadWorkspace: Boolean
    get() = state.loadWorkspace
    set(value) {
      state.loadWorkspace = value
    }

  var saveWorkspace: Boolean
    get() = state.saveWorkspace
    set(value) {
      state.saveWorkspace = value
    }

  @Synchronized
  private fun fetchInterpreterPath(): String {
    return state.interpreterPath ?: getSuggestedPath().also { path ->
      state.interpreterPath = path
    }
  }

  class State : BaseState() {
    var interpreterPath by string()
    var loadWorkspace by property(false)
    var saveWorkspace by property(false)
    var packageBuildSettingsState by property<RPackageBuildSettings.State>()
  }

  companion object {
    private fun getSuggestedPath(): String {
      return runAsync { RInterpreterUtil.suggestHomePath() }.
        onError { Logger.getInstance(RSettings::class.java).error(it) }.
        blockingGet(RInterpreterUtil.DEFAULT_TIMEOUT) ?: ""
    }

    fun getInstance(project: Project): RSettings = project.service<RSettings>()
  }
}
