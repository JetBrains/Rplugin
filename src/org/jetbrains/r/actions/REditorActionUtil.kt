/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.parsing.RElementTypes
import org.jetbrains.r.psi.api.*

internal object REditorActionUtil {
  data class SelectedCode(val code: String, val file: VirtualFile, val range: TextRange)

  fun getSelectedCode(editor: Editor): SelectedCode? {
    val code = editor.selectionModel.selectedText
    val virtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
    if (code != null && !StringUtil.isEmptyOrSpaces(code)) {
      return SelectedCode(code, virtualFile, TextRange(editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd))
    }
    val startElememt = findFirstElementToEvaluate(editor) ?: return null
    val lastElementToExecute = lastElementToExecute(startElememt)
    val range = TextRange(startElememt.textRange.startOffset, lastElementToExecute.textRange.endOffset)

    val text = range.subSequence(editor.document.charsSequence).toString()
    if (StringUtil.isEmptyOrSpaces(text)) return null
    val result = SelectedCode(text, virtualFile, range)

    val nextSibling = nextElement(lastElementToExecute)
    if (nextSibling != null) {
      val siblingEndPos = nextSibling.textOffset
      editor.caretModel.currentCaret.moveToOffset(siblingEndPos)
      editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }
    return result
  }

  private fun findFirstElementToEvaluate(editor: Editor): RExpression? {
    val project = editor.project ?: return null
    val document = editor.document
    val file = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return null
    val charsSequence = document.charsSequence

    var offset: Int = findLineStart(editor.caretModel.offset, charsSequence)

    while (true) {
      var element: PsiElement = file.findElementAt(offset) ?: return null
      //find first meaningful element
      while (element is PsiWhiteSpace || element is PsiComment) {
        val nextSibling: PsiElement = element.nextSibling ?: return null
        element = nextSibling
      }

      val candidate = findExecuteUnit(element) ?: return null

      val candidateOffset = findLineStart(candidate.textOffset, charsSequence)
      if (offset == candidateOffset) {
        // candidate is the first expression in the line. Return it!
        return candidate
      }
      // There is some previous expression peak it also
      offset = candidateOffset
    }
  }

  private fun findLineStart(offset: Int, charsSequence: CharSequence): Int {
    var i = offset - 1
    while (i >= 0) {
      if (charsSequence[i] == '\n')
        return i + 1
      i--
    }
    return 0
  }

  // Extend element to executable range. It should be maximal expression except it is a body of if/else/for/while/repeat
  // and that body is on separate line (findLineStart is checked the last condition by the fact)
  private fun findExecuteUnit(element: PsiElement): RExpression? {
    return PsiTreeUtil.findFirstParent(element) l@{ psiElement ->
      psiElement is RExpression && when (val parent = psiElement.parent) {
        is RFunctionExpression -> parent.expression == psiElement
        is RWhileStatement -> parent.body == psiElement
        is RIfStatement -> parent.ifBody == psiElement || parent.elseBody == psiElement
        is RRepeatStatement -> parent.body == psiElement
        is RForStatement -> parent.body == psiElement
        else -> parent is RBlockExpression || parent is RFile
      }
    } as RExpression?
  }

  // Find a last expression in expressions chain separated by `;`
  private fun lastElementToExecute(start: RExpression): RExpression {
    var current: PsiElement = start
    var result: RExpression = start
    while (true) {
      var next = current.nextSibling
      var parent = current.parent
      while (next == null) {
        next = parent ?: return result
        parent = next.parent
        next = next.nextSibling
      }
      if (next.elementType == RElementTypes.R_NL) {
        return result
      }
      val executeUnit = findExecuteUnit(next)
      if (executeUnit == null) {
        current = next
      }
      else {
        result = executeUnit
        current = executeUnit
      }
    }
  }

  private fun nextElement(evalElement: PsiElement): RExpression? {
    var current = evalElement
    while (true) {
      val result = PsiTreeUtil.getNextSiblingOfType(current, RExpression::class.java)
      if(result != null)
        return result
      current = current.parent ?: return null
    }
  }

  fun isRunningCommand(console: RConsoleView?, allowDebug: Boolean = false): Boolean {
    if (console == null) return false
    return (!allowDebug && console.debugger.isEnabled) || console.isRunningCommand
  }

  fun isRunningCommand(project: Project?, allowDebug: Boolean = false): Boolean {
    if (project == null) return false
    return isRunningCommand(RConsoleManager.getInstance(project).currentConsoleOrNull, allowDebug)
  }


  fun executeActionById(actionId: String, project: Project) {
    val action = ActionManager.getInstance().getAction(actionId) ?: throw IllegalStateException("No action ")
    invokeLater {
      DataManager.getInstance().dataContextFromFocusAsync.onSuccess { dataContext ->
        TransactionGuard.submitTransaction(project, Runnable {
          val event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, dataContext)
          ActionUtil.performActionDumbAwareWithCallbacks(action, event, dataContext)
        })
      }
    }
  }
}