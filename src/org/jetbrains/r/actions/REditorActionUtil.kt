/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.console.RConsoleExecuteActionHandler
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.*

internal object REditorActionUtil {
  data class SelectedCode(val code: String, val file: VirtualFile, val range: TextRange)

  fun getSelectedCode(editor: Editor): SelectedCode? {
    val code = editor.selectionModel.selectedText
    val virtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
    if (code != null && !StringUtil.isEmptyOrSpaces(code)) {
      return SelectedCode(code, virtualFile, TextRange(editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd))
    }
    val project = editor.project ?: return null
    val document = editor.document
    val file = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return null
    val content = document.charsSequence
    val startElement = findFirstElementToEvaluate(editor.caretModel.offset, content, file) ?: return null
    val (result, nextSibling) = select(startElement, content, virtualFile) ?: return null
    if (nextSibling != null) {
      val siblingEndPos = nextSibling.textOffset
      editor.caretModel.currentCaret.moveToOffset(siblingEndPos)
      editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }
    return result
  }

  fun getSelectedCode(caretOffset: Int,
                      content: CharSequence,
                      file: PsiFile): Pair<SelectedCode, PsiElement?>? {
    val virtualFile = file.virtualFile ?: return null
    val startElement = findFirstElementToEvaluate(caretOffset, content, file) ?: return null
    return select(startElement, content, virtualFile)
  }

  private fun select(startElement: RExpression,
                     charsSequence: CharSequence,
                     virtualFile: VirtualFile): Pair<SelectedCode, PsiElement?>? {
    val lastElementToExecute = lastElementToExecute(startElement)
    val range = TextRange(startElement.textRange.startOffset, lastElementToExecute.textRange.endOffset)

    val text = range.subSequence(charsSequence).toString()
    if (StringUtil.isEmptyOrSpaces(text)) return null
    return SelectedCode(text, virtualFile, range) to nextElement(lastElementToExecute)
  }

  private fun findFirstElementToEvaluate(caretOffset: Int,
                                         charsSequence: CharSequence,
                                         file: PsiFile): RExpression? {
    var offset: Int = findLineStart(caretOffset, charsSequence)

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
        if (parent is PsiFile) return result
        next = parent ?: return result
        parent = next.parent
        next = next.nextSibling
      }
      if (RPsiUtil.isWhitespaceWithNL(next)) {
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
      if (current is PsiFile) return null
      val result = PsiTreeUtil.getNextSiblingOfType(current, RExpression::class.java)
      if(result != null)
        return result
      current = current.parent ?: return null
    }
  }

  fun isRunningCommand(console: RConsoleView?): Boolean {
    return console != null && console.executeActionHandler.state != RConsoleExecuteActionHandler.State.PROMPT
  }

  fun isRunningCommand(project: Project?): Boolean {
    if (project == null) return false
    return isRunningCommand(RConsoleManager.getInstance(project).currentConsoleOrNull)
  }
}