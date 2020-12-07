// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.interpreter.RInterpreterLocation
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.RLocalInterpreterLocation
import org.jetbrains.r.interpreter.toLocalPathOrNull

@State(name = "RSettings", storages = [Storage("rSettings.xml")])
class RSettings(private val project: Project) : SimplePersistentStateComponent<RSettings.State>(State()) {
  private val interpreterLocationListeners = mutableListOf<RInterpreterLocationListener>()

  var interpreterLocation: RInterpreterLocation?
    get() = RInterpreterSettingsProvider.getProviders().asSequence().mapNotNull { it.getLocationFromState(state) }.firstOrNull()
    set(value) {
      if (value != null) {
        RInterpreterSettingsProvider.getProviders().forEach {
          if (it.putLocationToState(state, value)) {
            fireListeners(value)
            return
          }
        }
      }
      state.setNoInterpreter()
      fireListeners(null)
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

  var disableRprofile: Boolean
    get() = state.disableRprofile
    set(value) {
      state.disableRprofile  = value
    }

  var rStudioApiEnabled: Boolean
    get() = state.RStudioApiEnabled
    set(value) {
      state.RStudioApiEnabled  = value
    }

  fun addInterpreterLocationListener(listener: RInterpreterLocationListener, parentDisposable: Disposable? = null) {
    interpreterLocationListeners.add(listener)
    if (parentDisposable != null) {
      Disposer.register(parentDisposable, Disposable { interpreterLocationListeners.remove(listener) })
    }
  }

  @Synchronized
  private fun fetchInterpreterLocation(): RInterpreterLocation? {
    return getSuggestedPath()
      ?.let { RLocalInterpreterLocation(it) }
      ?.also { state.interpreterPath = it.toLocalPathOrNull() ?: "" }
  }

  private fun fireListeners(actualInterpreterLocation: RInterpreterLocation?) {
    interpreterLocationListeners.forEach { it.projectInterpreterLocationChanged(actualInterpreterLocation) }
  }

  interface RInterpreterLocationListener {
    fun projectInterpreterLocationChanged(actualInterpreterLocation: RInterpreterLocation?)
  }

  class State : BaseState() {
    var interpreterPath by string()
    var remoteHost by string()
    var remoteBasePath by string()
    var loadWorkspace by property(false)
    var saveWorkspace by property(false)
    var disableRprofile by property(false)
    var RStudioApiEnabled by property(true)
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
