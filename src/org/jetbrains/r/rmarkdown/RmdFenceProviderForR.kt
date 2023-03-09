/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.Indent
import com.intellij.lang.Language
import com.intellij.openapi.util.TextRange
import com.intellij.psi.tree.IElementType
import com.intellij.quarto.QmdFenceProvider
import org.jetbrains.r.RLanguage
import org.jetbrains.r.editor.formatting.RFormatterBlock
import org.jetbrains.r.editor.formatting.RFormattingContext
import org.jetbrains.r.editor.formatting.TemplateContext

class RmdFenceProviderForR : RmdFenceProvider, QmdFenceProvider {
  override val fenceLanguage: Language = RLanguage.INSTANCE
  override val fenceElementType: IElementType = R_FENCE_ELEMENT_TYPE

  override fun getFormattingBlocks(context: TemplateContext, textRange: TextRange): List<Block>? {
    return context.getFenceBlocks(textRange, RLanguage.INSTANCE) { roots ->
      val rContext = RFormattingContext(context.settings)
      roots.map { node -> RFormatterBlock(rContext, node) }
    }
  }

  override fun getNewChildAttributes(newChildIndex: Int): ChildAttributes = ChildAttributes(Indent.getNoneIndent(), null)
}
