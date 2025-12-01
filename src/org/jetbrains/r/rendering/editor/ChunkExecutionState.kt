package org.jetbrains.r.rendering.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.r.console.RConsoleManagerImpl
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class ChunkExecutionState(
  val editor: Editor,
  val terminationRequired: AtomicBoolean = AtomicBoolean(),
  val isDebug: Boolean = false,
  val currentPsiElement: AtomicReference<PsiElement> = AtomicReference(),
  val interrupt: AtomicReference<() -> Unit> = AtomicReference(),
) {
  inline fun <T> useCurrent(f: () -> T): T {
    val project = editor.project

    if (project != null) {
      setCurrent(project, this)
    }
    try {
      return f()
    }
    finally {
      if (project != null) {
        setCurrent(project, null)
      }
    }
  }

  companion object {
    fun setCurrent(project: Project, value: ChunkExecutionState?) {
      RConsoleManagerImpl.Companion.getInstance(project).currentConsoleOrNull?.executeActionHandler?.chunkState = value
    }

    fun getCurrent(project: Project): ChunkExecutionState? =
      RConsoleManagerImpl.Companion.getInstance(project).currentConsoleOrNull?.executeActionHandler?.chunkState
  }
}

val Editor.chunkExecutionState: ChunkExecutionState?
  get() = project?.chunkExecutionState

val Project.chunkExecutionState: ChunkExecutionState?
  get() = ChunkExecutionState.getCurrent(this)