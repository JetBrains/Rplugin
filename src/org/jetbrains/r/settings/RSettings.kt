// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.interpreter.RInterpreterLocation
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.RLocalInterpreterLocation
import org.jetbrains.r.interpreter.toLocalPathOrNull

@State(name = "RSettings", storages = [Storage("rSettings.xml")])
class RSettings(private val project: Project) : SimplePersistentStateComponent<RSettings.State>(State()) {
  var interpreterLocation: RInterpreterLocation?
    get() = RInterpreterSettingsProvider.getProviders().asSequence().mapNotNull { it.getLocationFromState(state) }.firstOrNull()
    set(value) {
      if (value != null) {
        RInterpreterSettingsProvider.getProviders().forEach {
          if (it.putLocationToState(state, value)) {
            return
          }
        }
      }
      state.setNoInterpreter()
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
  private fun fetchInterpreterLocation(): RInterpreterLocation? {
    return getSuggestedPath()
      ?.let { RLocalInterpreterLocation(it) }
      ?.also { state.interpreterPath = it.toLocalPathOrNull() ?: "" }
  }

  class State : BaseState() {
    var interpreterPath by string()
    var remoteHost by string()
    var loadWorkspace by property(false)
    var saveWorkspace by property(false)
    var packageBuildSettingsState by property<RPackageBuildSettings.State>()

    fun setNoInterpreter() {
      interpreterPath = null
      remoteHost = null
    }
  }

  companion object {
    private fun getSuggestedPath(): String? {
      return runAsync { RInterpreterUtil.suggestHomePath() }
        .onError { Logger.getInstance(RSettings::class.java).error(it) }
        .blockingGet(RInterpreterUtil.DEFAULT_TIMEOUT)
        ?.takeIf { it.isNotBlank() }
    }

    fun getInstance(project: Project): RSettings = project.service<RSettings>()
  }
}
