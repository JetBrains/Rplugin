/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.r.RLanguage
import org.jetbrains.r.actions.RPromotedAction
import org.jetbrains.r.actions.editor
import org.jetbrains.r.rmarkdown.RMarkdownLanguage

abstract class RInsertActionBase(private val symbol: String) : DumbAwareAction(), RPromotedAction {
  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.editor ?: return
    val document = editor.document
    val project = e.project ?: return
    val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
    val language = psiFile?.language
    if (language != RLanguage.INSTANCE && language != RMarkdownLanguage) return
    val offset = editor.caretModel.offset
    val addSpaces = CodeStyle.getSettings(editor).getCommonSettings(RLanguage.INSTANCE).SPACE_AROUND_ASSIGNMENT_OPERATORS

    val insert = if (addSpaces && offset > 0)
      if (!document.charsSequence[offset - 1].isWhitespace()) " $symbol "
      else "$symbol "
    else symbol

    runWriteAction {
      CommandProcessor.getInstance().executeCommand(e.project, Runnable {
        document.insertString(offset, insert)
      }, templateText, null)
      editor.caretModel.moveToOffset(offset + insert.length)
    }
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    val editor = e.editor ?: return
    val document = editor.document
    val project = e.project ?: return
    val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
    val language = psiFile?.language
    presentation.isEnabled = language == RLanguage.INSTANCE || language == RMarkdownLanguage
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

class RInsertAssignmentAction : RInsertActionBase("<-")
class RInsertPipeAction : RInsertActionBase("%>%")
