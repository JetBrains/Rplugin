/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class RGraphicsRepository(private val project: Project) {
  private val states = mutableSetOf<RGraphicsState>()
  private val snapshotListeners = mutableListOf<(RSnapshotsUpdate) -> Unit>()

  private val stateListener = object : RGraphicsState.Listener {
    override fun onCurrentChange(update: RSnapshotsUpdate) {
      notifySnapshots(update)
    }

    override fun onReset() {
      notifySnapshots(RSnapshotsUpdate.empty)
    }
  }

  private var currentState: RGraphicsState? = null

  @Synchronized
  fun setActiveState(state: RGraphicsState) {
    if (state !in states) {
      states.add(state)
      state.addListener(stateListener)
    }
    currentState = state
    notifySnapshots(state.snapshots)
  }

  @Synchronized
  fun addSnapshotListener(listener: (RSnapshotsUpdate) -> Unit) {
    snapshotListeners.add(listener)
    currentState?.let {
      listener(it.snapshots)
    }
  }

  @Synchronized
  fun clearSnapshot(number: Int) {
    currentState?.clearSnapshot(number)
  }

  @Synchronized
  fun clearAllSnapshots() {
    currentState?.reset()
  }

  @Synchronized
  fun getScreenParameters(): RGraphicsUtils.ScreenParameters? {
    return currentState?.screenParameters?.currentValue
  }

  @Synchronized
  fun setScreenParameters(parameters: RGraphicsUtils.ScreenParameters) {
    currentState?.changeScreenParameters(parameters)
  }

  @Synchronized
  fun setCurrentSnapshotNumber(number: Int?) {
    currentState?.let { state ->
      state.currentSnapshotNumber = number
    }
  }

  private fun notifySnapshots(update: RSnapshotsUpdate) {
    for (listener in snapshotListeners) {
      listener(update)
    }
  }

  companion object {
    fun getInstance(project: Project) = project.service<RGraphicsRepository>()
  }
}