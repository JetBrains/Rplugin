/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.DumbAwareAction
import org.jetbrains.r.RLanguage
import org.jetbrains.r.actions.RPromotedAction
import org.jetbrains.r.actions.editor
import org.jetbrains.r.actions.psiFile
import org.jetbrains.r.rmarkdown.RMarkdownLanguage

class RInsertAssignmentAction : DumbAwareAction(), RPromotedAction {
  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.editor ?: return
    val language = e.psiFile?.language
    if (language != RLanguage.INSTANCE && language != RMarkdownLanguage) return
    val offset = editor.caretModel.offset
    val document = editor.document
    val addSpaces = CodeStyle.getSettings(editor).getCommonSettings(RLanguage.INSTANCE).SPACE_AROUND_ASSIGNMENT_OPERATORS

    val insert = if (addSpaces && offset > 0)
      if (!document.charsSequence[offset - 1].isWhitespace()) " <- "
      else "<- "
    else "<-"

    runWriteAction {
      CommandProcessor.getInstance().executeCommand(e.project, Runnable {
        document.insertString(offset, insert)
      }, templateText, null)
      editor.caretModel.moveToOffset(offset + insert.length)
    }
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    val language = e.psiFile?.language
    presentation.isEnabled = language == RLanguage.INSTANCE || language == RMarkdownLanguage
  }
}
