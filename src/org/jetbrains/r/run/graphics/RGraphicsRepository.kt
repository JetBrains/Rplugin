/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.io.File

class RGraphicsRepository(private val project: Project) {
  private val states = mutableSetOf<RGraphicsState>()
  private val snapshotListeners = mutableListOf<(List<File>) -> Unit>()

  private val stateListener = object : RGraphicsState.Listener {
    override fun onCurrentChange(snapshots: List<File>) {
      notifySnapshots(snapshots)
    }

    override fun onReset() {
      notifySnapshots(listOf())
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
  fun addSnapshotListener(listener: (List<File>) -> Unit) {
    snapshotListeners.add(listener)
    currentState?.let {
      listener(it.snapshots)
    }
  }

  @Synchronized
  fun clearSnapshot(index: Int) {
    currentState?.clearSnapshot(index)
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

  private fun notifySnapshots(snapshots: List<File>) {
    for (listener in snapshotListeners) {
      listener(snapshots)
    }
  }

  companion object {
    fun getInstance(project: Project) = project.service<RGraphicsRepository>()
  }
}