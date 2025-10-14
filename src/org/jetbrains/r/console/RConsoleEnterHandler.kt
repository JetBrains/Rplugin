/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.r.psi.parsing.RElementTypes
import com.intellij.r.psi.psi.RRecursiveElementVisitor
import com.intellij.r.psi.psi.api.RCallExpression
import org.jetbrains.r.statistics.RWorkflowCollector.logConsoleMethodCall

object RConsoleEnterHandler {

  /**
   * if the [editor]'s content is not ready ENTER will be send to [editor]
   *
   * @return true if the [editor]'s content is ready to be send to REPL or false otherwise
   */
  fun handleEnterPressed(editor: EditorEx): Boolean {
    val project = editor.project ?: throw IllegalArgumentException()
    val lineCount = editor.document.lineCount
    val caretPosition = editor.caretModel.logicalPosition

    if (caretPosition.line != lineCount - 1) {
      return true
    }

    editor.selectionModel.removeSelection()
    if (caretPosition.line == lineCount - 1) {
      // we can move caret if only it's on the last line of command
      val lineEndOffset = editor.document.getLineEndOffset(caretPosition.line)
      editor.caretModel.moveToOffset(lineEndOffset)
    }
    val psiMgr = PsiDocumentManager.getInstance(project)
    psiMgr.commitDocument(editor.document)

    val caretOffset = editor.expectedCaretOffset
    val atElement = findFirstNoneSpaceElement(psiMgr.getPsiFile(editor.document)!!, caretOffset)
    if (atElement == null) {
      executeEnterHandler(project, editor)
      return false
    }

    val isAtTheEndOfCommand = editor.document.getLineNumber(caretOffset) == editor.document.lineCount - 1
    val hasCompleteStatement = checkComplete(atElement)
    if (isAtTheEndOfCommand && hasCompleteStatement) {
      return true
    }
    executeEnterHandler(project, editor)
    return false
  }

  fun executeEnterHandler(project: Project, editor: EditorEx) {
    val enterHandler = EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_ENTER)
    WriteCommandAction.runWriteCommandAction(project) {
      enterHandler.execute(editor, null, DataManager.getInstance().getDataContext(editor.component))
    }
  }

  fun analyzePrompt(consoleView: RConsoleView) {
    val file = consoleView.file
    file.accept(object : RRecursiveElementVisitor() {
      override fun visitCallExpression(call: RCallExpression) {
        super.visitCallExpression(call)
        val name = call.expression.text
        logConsoleMethodCall(consoleView.project, name)
      }
    })
  }

  private fun checkComplete(el: PsiElement): Boolean {
    val file = el.containingFile
    if (el.elementType == RElementTypes.R_INVALID_STRING) return false
    return !PsiTreeUtil.hasErrorElements(file)
  }

  private fun findFirstNoneSpaceElement(psiFile: PsiFile, offset: Int): PsiElement? {
    for (i in offset downTo 0) {
      val el = psiFile.findElementAt(i)
      if (el != null && el !is PsiWhiteSpace) {
        return el
      }
    }
    return null
  }
}