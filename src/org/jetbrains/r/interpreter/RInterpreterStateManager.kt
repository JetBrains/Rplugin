/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.console.RConsoleManager

interface RInterpreterStateManager {
  val currentStateOrNull: RInterpreterState?
  val states: List<RInterpreterState>

  fun getCurrentStateAsync(): Promise<RInterpreterState>

  fun getCurrentStateBlocking(timeout: Int) = getCurrentStateAsync().run {
    try {
      blockingGet(timeout)
    }
    catch (t: Throwable) {
      null
    }
  }

  companion object {
    fun getInstance(project: Project): RInterpreterStateManager = project.getService(RInterpreterStateManager::class.java)
    fun getInstanceIfCreated(project: Project): RInterpreterStateManager? = project.getServiceIfCreated(RInterpreterStateManager::class.java)

    fun getCurrentStateAsync(project: Project): Promise<RInterpreterState> = getInstance(project).getCurrentStateAsync()

    fun getCurrentStateOrNull(project: Project): RInterpreterState? = getInstanceIfCreated(project)?.currentStateOrNull
    fun getCurrentStateBlocking(project: Project, timeout: Int): RInterpreterState? = getInstance(project).getCurrentStateBlocking(timeout)
  }
}

class RInterpreterStateManagerImpl(private val project: Project) : RInterpreterStateManager {
  private val rConsoleManager
    get() = RConsoleManager.getInstance(project)

  override val states: List<RInterpreterState>
    get() = rConsoleManager.consoles.map { it.rInterop.state }

  override val currentStateOrNull: RInterpreterState?
    get() = rConsoleManager.currentConsoleOrNull?.rInterop?.state

  override fun getCurrentStateAsync(): Promise<RInterpreterState> {
    return rConsoleManager.currentConsoleAsync.then { it.rInterop.state }
  }
}