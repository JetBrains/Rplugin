/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.lang.ASTNode
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.impl.source.tree.TreeUtil.findLastLeaf
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownCodeFence
import org.jetbrains.r.actions.RPromotedAction
import org.jetbrains.r.actions.caret
import org.jetbrains.r.actions.editor
import org.jetbrains.r.actions.psiFile

class RMarkdownNewChunk : DumbAwareAction(), RPromotedAction {
  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.editor ?: return
    val offset = e.caret?.offset ?: return
    val file = e.psiFile ?: return
    val atOffset = { i: Int -> file.node.findLeafElementAt(i) }
    val document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return

    val whereToInsert: Int
    if (document.charsSequence.isEmpty()) {
      whereToInsert = 0
    }
    else {
      val leafAtCaret = (atOffset(offset) ?: findLastLeaf(file.node))?.psi ?: return
      val markdownCodeFence = PsiTreeUtil.getParentOfType(leafAtCaret, MarkdownCodeFence::class.java)

      whereToInsert = when {
        markdownCodeFence != null -> markdownCodeFence.textRange.endOffset
        isMarkdownEol(TreeUtil.prevLeaf(leafAtCaret.node)) && offset == leafAtCaret.textRange.startOffset -> offset
        else -> findNextEol(leafAtCaret)
      }
    }

    val endOfInsertString = if (isMarkdownEol(atOffset(whereToInsert))) "" else "\n"
    val startOfInsertString = if (whereToInsert >= 1 && !isMarkdownEol(atOffset(whereToInsert - 1))) "\n" else ""

    runWriteAction {
      val startStr = startOfInsertString + "```{r}\n"
      val runnable = Runnable {
        document.insertString(whereToInsert, startStr + "\n```" + endOfInsertString)
      }
      CommandProcessor.getInstance().executeCommand(file.project, runnable, templateText, null)
      editor.caretModel.moveToOffset(whereToInsert + startStr.length)
    }
  }

  /** IMPORTANT: PSI has standard TokenType.WHITE_SPACE but lexical level use MARKDOWN_EOL. */
  private fun isMarkdownEol(psi: ASTNode?): Boolean = psi != null && psi.elementType == TokenType.WHITE_SPACE && psi.textContains('\n')

  private fun findNextEol(start: PsiElement): Int {
    var node: ASTNode = start.node

    while (true) {
      if (isMarkdownEol(node)) {
        return node.textRange.startOffset
      }
      node = TreeUtil.nextLeaf(node) ?: return node.textRange.endOffset
    }
  }


  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.getData(CommonDataKeys.VIRTUAL_FILE)?.fileType == RMarkdownFileType
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}