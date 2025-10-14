package com.intellij.r.psi.interpreter

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.Promise

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
    fun getInstance(project: Project): RInterpreterStateManager = project.service()
    private fun getInstanceIfCreated(project: Project): RInterpreterStateManager? = project.getServiceIfCreated(RInterpreterStateManager::class.java)

    fun getCurrentStateAsync(project: Project): Promise<RInterpreterState> = getInstance(project).getCurrentStateAsync()

    fun getCurrentStateOrNull(project: Project): RInterpreterState? = getInstanceIfCreated(project)?.currentStateOrNull
    fun getCurrentStateBlocking(project: Project, timeout: Int): RInterpreterState? = getInstance(project).getCurrentStateBlocking(timeout)
  }
}