// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova

/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.highlighting

import com.intellij.lang.Language
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.RainbowColorSettingsPage
import org.intellij.plugins.markdown.highlighting.MarkdownSyntaxHighlighter
import org.jetbrains.r.R_MARKDOWN
import org.jetbrains.r.rmarkdown.RMarkdownLanguage
import javax.swing.Icon

class RMarkdownColorSettingsPage : RainbowColorSettingsPage {

  override fun getIcon(): Icon? = R_MARKDOWN

  override fun isRainbowType(type: TextAttributesKey?): Boolean = type == LOCAL_VARIABLE || type == PARAMETER || type == CLOSURE

  override fun getHighlighter(): SyntaxHighlighter = MarkdownSyntaxHighlighter()

  override fun getDemoText(): String = R_MARKDOWN_DEMO

  override fun getLanguage(): Language = RMarkdownLanguage

  override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = mapOf("chunk" to RMARKDOWN_CHUNK)

  override fun getAttributeDescriptors(): Array<AttributesDescriptor> = arrayOf(AttributesDescriptor("R Markdown Chunk", RMARKDOWN_CHUNK))

  override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

  override fun getDisplayName(): String = "R Markdown"

  companion object {
    private val R_MARKDOWN_DEMO = """
      Hello world
      
      <chunk>
      ```{r}
       
      ```
      </chunk>
    """.trimIndent()
  }
}
