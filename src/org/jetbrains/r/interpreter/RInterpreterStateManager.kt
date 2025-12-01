/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.openapi.project.Project
import com.intellij.r.psi.interpreter.RInterpreterState
import com.intellij.r.psi.interpreter.RInterpreterStateManager
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.console.RConsoleManagerImpl

class RInterpreterStateManagerImpl(private val project: Project) : RInterpreterStateManager {
  private val rConsoleManager
    get() = RConsoleManagerImpl.getInstance(project)

  override val states: List<RInterpreterState>
    get() = rConsoleManager.consoles.map { it.rInterop.state }

  override val currentStateOrNull: RInterpreterState?
    get() = rConsoleManager.currentConsoleOrNull?.rInterop?.state

  override fun getCurrentStateAsync(): Promise<RInterpreterState> {
    return rConsoleManager.currentConsoleAsync.then { it.rInterop.state }
  }
}