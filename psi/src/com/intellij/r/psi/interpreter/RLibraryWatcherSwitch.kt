/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.interpreter

class RLibraryWatcherSwitch {
  private var isActive = true
  private var lastAction: (() -> Unit)? = null

  fun enable() {
    val action = enableAndGetLastAction()
    action?.invoke()
  }

  @Synchronized
  private fun enableAndGetLastAction(): (() -> Unit)? {
    isActive = true
    return lastAction.also {
      lastAction = null
    }
  }

  @Synchronized
  fun disable() {
    isActive = false
  }

  fun onActive(action: () -> Unit) {
    if (checkOrPostponeAction(action)) {
      action()
    }
  }

  @Synchronized
  private fun checkOrPostponeAction(action: () -> Unit): Boolean {
    if (!isActive) {
      lastAction = action
    }
    return isActive
  }
}
