/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.formatting.*
import com.intellij.lang.Language
import com.intellij.openapi.util.TextRange
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.IElementType
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.formatter.PyBlock
import com.jetbrains.python.formatter.PyBlockContext
import com.jetbrains.python.formatter.PythonFormattingModelBuilder
import org.jetbrains.r.editor.formatting.TemplateContext
import org.jetbrains.r.quarto.QmdFenceProvider

class RmdFenceProviderForPython : RmdFenceProvider, QmdFenceProvider {
  override val fenceLanguage: Language = PythonLanguage.INSTANCE
  override val fenceElementType: IElementType = IElementType("Python Fence", RMarkdownLanguage)

  override fun getFormattingBlocks(context: TemplateContext, textRange: TextRange): List<Block>? {
    return context.getFenceBlocks(textRange, PythonLanguage.INSTANCE) { roots ->
      val pyContext = PyBlockContext(context.settings, getPythonSpacingBuilder(context.settings), FormattingMode.REFORMAT)
      roots.map { node -> PyBlock(null, node, null, Indent.getNoneIndent(), null, pyContext) }
    }
  }

  override fun getNewChildAttributes(newChildIndex: Int): ChildAttributes = ChildAttributes.DELEGATE_TO_PREV_CHILD
}

private fun getPythonSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder {
  // A simple hack: PythonFormattingModelBuilder#createSpacingBuilder can be static method but now it is non-static and protected
  return object : PythonFormattingModelBuilder() {
    fun extractProtectedSpacingBuilder(): SpacingBuilder {
      return createSpacingBuilder(settings)
    }
  }.extractProtectedSpacingBuilder()
}
