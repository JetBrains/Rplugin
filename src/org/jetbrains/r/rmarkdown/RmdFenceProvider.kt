/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.rmarkdown

import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.TextRange
import com.intellij.psi.tree.IElementType
import org.jetbrains.r.editor.formatting.TemplateContext
import java.util.Locale

interface RmdFenceProvider {
  val fenceLanguage: Language

  /** Element type in Markdown language to identify Markdown token with executable code (without header and backticks) */
  val fenceElementType: IElementType

  fun getFormattingBlocks(context: TemplateContext, textRange: TextRange): List<Block>?

  /** Indent and alignment settings which are applied to a new child block inside Code Fence */
  fun getNewChildAttributes(newChildIndex: Int): ChildAttributes

  companion object {
    val EP_NAME: ExtensionPointName<RmdFenceProvider> = ExtensionPointName.create("com.intellij.rmdFenceProvider")

    fun find(predicate: (RmdFenceProvider) -> Boolean): RmdFenceProvider? {
      return EP_NAME.extensionList.firstOrNull(predicate)
    }

    fun matchHeader(fullFenceHeader: CharSequence): RmdFenceProvider? {
      val language = RMarkdownPsiUtil.getExecutableFenceLanguage(fullFenceHeader) ?: return null
      return find { language.lowercase(Locale.getDefault()) == it.fenceLanguage.displayName.lowercase(Locale.getDefault()) }
    }
  }
}
