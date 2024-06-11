package org.jetbrains.r.rendering.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.r.console.RConsoleManager
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class ChunkExecutionState(private val editor: Editor,
                          val terminationRequired: AtomicBoolean = AtomicBoolean(),
                          val isDebug: Boolean = false,
                          val currentPsiElement: AtomicReference<PsiElement> = AtomicReference(),
                          val interrupt: AtomicReference<() -> Unit> = AtomicReference())

var Editor.chunkExecutionState: ChunkExecutionState?
  get() = project?.chunkExecutionState
  set(value) {
    project?.chunkExecutionState = value
  }

var Project.chunkExecutionState: ChunkExecutionState?
  get() = RConsoleManager.Companion.getInstance(this).currentConsoleOrNull?.executeActionHandler?.chunkState
  set(value) {
    RConsoleManager.Companion.getInstance(this).currentConsoleOrNull?.executeActionHandler?.chunkState = value
  }