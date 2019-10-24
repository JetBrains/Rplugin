/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.icons.AllIcons
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.RFileType
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.console.RConsoleUtil
import org.jetbrains.r.psi.api.*


/**
 * Event handler for the "Run Selection" action within an Arc code editor - runs the currently selected text within the
 * current REPL.
 */
class EvaluateInConsole : REditorActionBase(
  "Run in Console",
  "Run Selection/Line in Console",
  AllIcons.Actions.Execute) {

  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.getData(PlatformDataKeys.EDITOR) ?: return

    val project = editor.project ?: throw RuntimeException("no project in $e")

    var code = editor.selectionModel.selectedText
    val psiFile =e.psiFile ?: return

    if (StringUtil.isEmptyOrSpaces(code)) {
      var element = getCaretElement(editor, psiFile)
      // resolve injected chunk elements if file-type is not R
      if (psiFile.fileType != RFileType) {
        element = InjectedLanguageManager.getInstance(project).findInjectedElementAt(psiFile, editor.caretModel.offset)
      }

      // grow until we reach expression barrier
      val evalElement = PsiTreeUtil.findFirstParent(element) { psiElement ->
        // avoid that { would be evaluated if cursor is set after block
        if (psiElement is LeafPsiElement) return@findFirstParent false

        val parent = psiElement.parent
        parent is RBlockExpression ||
        parent is RWhileStatement ||
        parent is RForStatement ||
        parent is RIfStatement ||
        parent is RFile
      } ?: return

      code = evalElement.text

      // set caret to next downstream element
      // todo add preference for caret transition after eval
      val nextSibling = PsiTreeUtil.getNextSiblingOfType(evalElement, RExpression::class.java)
      if (nextSibling != null) {
        val siblingEndPos = nextSibling.textOffset + nextSibling.textLength
        editor.caretModel.currentCaret.moveToOffset(siblingEndPos)
      }
    }

    // just proceed if we have any code to be evaluated
    if (StringUtil.isEmptyOrSpaces(code)) return

    val codeForExecution = code!!.trim { it <= ' ' }


    RConsoleManager.getInstance(project).currentConsoleAsync
      .onSuccess { runInEdt { runWriteAction { it.executeText(codeForExecution) } } }
      .onError { ex -> RConsoleUtil.notifyError(project, ex.message) }
    RConsoleToolWindowFactory.show(project)
  }


  private fun getCaretElement(editor: Editor, psiFile: PsiFile): PsiElement? {

    // why not PsiUtilBase.getElementAtCaret(editor) ??
    // todo rather fix and use https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000126084-Incorrect-help-when-caret-at-word-end

    // is caret at line start
    val caretModel = editor.caretModel
    val curCaret = caretModel.currentCaret

    // return nothing if caret is in empty line
    // FIXME lines that just contain whitespace are not ignored
    if (curCaret.visualLineEnd - curCaret.visualLineStart == 0) return null

    // TODO isn't there a an existing utility method for this? IJ does for most editor actions
    //        TargetElementUtil.findTargetElement(editor, curCaret.getOffset())
    var element = psiFile.findElementAt(caretModel.offset)

    if (element == null && caretModel.offset > 0) {
      element = psiFile.findElementAt(caretModel.offset - 1)
    }

    if (element == null) return null


    if (curCaret.visualLineStart == curCaret.offset) {
      return PsiTreeUtil.nextVisibleLeaf(element)
    }

    return if (curCaret.visualLineEnd - 1 == curCaret.offset) {
      PsiTreeUtil.prevVisibleLeaf(element)
    }
    else element
  }
}
