/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.r.RLanguage

class RBackspaceHandler : BackspaceHandlerDelegate() {
  override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) {
    if (file.language != RLanguage.INSTANCE) return

    val caretPosition = editor.caretModel.offset
    val document = editor.document
    val text = document.charsSequence

    fun beforePosition(prefix: String): Int =
      if (caretPosition >= prefix.length && text.subSequence(caretPosition - prefix.length, caretPosition).toString() == prefix) {
        prefix.length
      } else {
        0
      }

    if (caretPosition != text.length && text[caretPosition] != '\n') return

    val prefixLen = beforePosition("#'") + beforePosition("#' ")
    if (prefixLen == 0) return
    if (!isLineStart(caretPosition, prefixLen, text)) return

    document.deleteString(caretPosition - prefixLen + 1, caretPosition)
  }

  private fun isLineStart(caretPosition: Int, prefixLen: Int, text: CharSequence): Boolean {
    var idx = caretPosition - prefixLen - 1
    loop@ while (idx >= 0) {
      when (text[idx]) {
        ' ', '\t' -> idx--
        '\n' -> break@loop
        else -> return false
      }
    }
    return true
  }

  override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean = false
}
