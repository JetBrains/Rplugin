/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen

import com.intellij.codeInsight.editorActions.EnterHandler
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil

class RoxygenEnterDelegateToHostHandler : EnterHandlerDelegateAdapter() {
  override fun preprocessEnter(file: PsiFile,
                               editor: Editor,
                               caretOffsetRef: Ref<Int>,
                               caretAdvance: Ref<Int>,
                               dataContext: DataContext,
                               originalHandler: EditorActionHandler?): EnterHandlerDelegate.Result {
    val language = EnterHandler.getLanguage(dataContext)
    if (language !is RoxygenLanguage || originalHandler == null) {
      return EnterHandlerDelegate.Result.Continue
    }
    val topEditor = InjectedLanguageUtil.getTopLevelEditor(editor)
    originalHandler.execute(topEditor, topEditor.caretModel.currentCaret, dataContext)
    return EnterHandlerDelegate.Result.Stop
  }
}