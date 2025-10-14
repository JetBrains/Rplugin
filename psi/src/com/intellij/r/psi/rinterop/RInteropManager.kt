package com.intellij.r.psi.rinterop

import com.intellij.openapi.project.Project

interface RInteropManager {
  fun currentConsoleInterop(): RInterop?
  suspend fun currentConsoleInteropOrStart(): RInterop

  companion object {
    fun getInstance(project: Project): RInteropManager = project.getService(RInteropManager::class.java)
  }
}