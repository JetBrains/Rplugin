package org.jetbrains.r.rinterop

import com.intellij.openapi.project.Project
import com.intellij.r.psi.rinterop.RInterop
import com.intellij.r.psi.rinterop.RInteropManager
import org.jetbrains.r.console.RConsoleManagerImpl

class RInteropManagerImpl(
  private val project: Project,
) : RInteropManager {
  override fun currentConsoleInterop(): RInterop? {
    return RConsoleManagerImpl.getInstance(project).currentConsoleOrNull?.rInterop
  }

  override suspend fun currentConsoleInteropOrStart(): RInterop {
    return RConsoleManagerImpl.getInstance(project).awaitCurrentConsole().getOrThrow().rInterop
  }
}