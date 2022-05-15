/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.hints.parameterInfo

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeInsight.hints.ParameterHintsPassFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.r.RLanguage
import org.jetbrains.r.rmarkdown.RMarkdownLanguage

class RMarkdownParameterHintsPassFactory : ParameterHintsPassFactory() {
  override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
    if (file.language != RMarkdownLanguage) return null
    return super.createHighlightingPass(file.viewProvider.getPsi(RLanguage.INSTANCE), editor)
  }
}
