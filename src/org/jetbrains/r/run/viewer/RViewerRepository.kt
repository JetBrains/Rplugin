/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.viewer

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class RViewerRepository(private val project: Project) {
  private val states = mutableSetOf<RViewerState>()
  private val urlListeners = mutableListOf<(String?) -> Unit>()

  private val stateListener = object : RViewerState.Listener {
    override fun onCurrentChange(newUrl: String) {
      notifyUrl(newUrl)
    }

    override fun onReset() {
      notifyUrl(null)
    }
  }

  private var currentState: RViewerState? = null

  @Synchronized
  fun setActiveState(state: RViewerState) {
    if (state !in states) {
      states.add(state)
      state.addListener(stateListener)
    }
    currentState = state
    notifyUrl(state.url)
  }

  @Synchronized
  fun addUrlListener(listener: (String?) -> Unit) {
    urlListeners.add(listener)
    currentState?.let {
      listener(it.url)
    }
  }

  private fun notifyUrl(url: String?) {
    for (listener in urlListeners) {
      listener(url)
    }
  }

  companion object {
    fun getInstance(project: Project) = project.service<RViewerRepository>()
  }
}
