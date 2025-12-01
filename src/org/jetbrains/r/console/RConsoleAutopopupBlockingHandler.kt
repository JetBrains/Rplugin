/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile

class RConsoleAutopopupBlockingHandler : TypedHandlerDelegate() {
  override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
    editor.getUserData(REPL_KEY)?.executeActionHandler?.state?.let {
      if (it == RConsoleExecuteActionHandler.State.READ_LN || it == RConsoleExecuteActionHandler.State.SUBPROCESS_INPUT) {
        return Result.STOP
      }
    }
    return Result.CONTINUE
  }

  companion object {
    val REPL_KEY: Key<RConsoleViewImpl> = Key("r.repl.console.editor")
  }
}
