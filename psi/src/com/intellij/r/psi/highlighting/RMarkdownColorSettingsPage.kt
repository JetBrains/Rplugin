// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova

/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.highlighting

import com.intellij.lang.Language
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.RainbowColorSettingsPage
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.icons.RIcons
import com.intellij.r.psi.rmarkdown.RMarkdownLanguage
import org.jetbrains.annotations.NonNls
import com.intellij.r.psi.highlighting.RColorSettingsPage.Companion.R_DEMO
import com.intellij.r.psi.highlighting.RColorSettingsPage.Companion.TAGS
import javax.swing.Icon

class RMarkdownColorSettingsPage : RainbowColorSettingsPage {

  override fun getIcon(): Icon = RIcons.RMarkdown

  override fun isRainbowType(type: TextAttributesKey?): Boolean = type == LOCAL_VARIABLE || type == PARAMETER || type == CLOSURE

  override fun getHighlighter(): SyntaxHighlighter = RHighlighter()

  override fun getDemoText(): String = R_MARKDOWN_DEMO

  override fun getLanguage(): Language = RMarkdownLanguage

  override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = tags

  override fun getAttributeDescriptors(): Array<AttributesDescriptor> = arrayOf(AttributesDescriptor(
    RBundle.message("attribute.descriptor.r.markdown.chunk"), RMARKDOWN_CHUNK))

  override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

  @NonNls
  override fun getDisplayName(): String = "R Markdown"

  companion object {

  private val tags = HashMap(TAGS).also {
    it["chunk"] = RMARKDOWN_CHUNK
  }

  private val demoWithLexerHighlighting: String
    get() {
      val textForLexer = R_DEMO.toCharArray()
      Regex("<[/_a-zA-Z]*>").findAll(R_DEMO).forEach {
        for (i in it.range.first..it.range.last) {
          textForLexer[i] = ' '
        }
      }
      val rHighlighter = RHighlighter()
      val highlightingLexer = rHighlighter.highlightingLexer
      highlightingLexer.start(String(textForLexer))
      highlightingLexer.advance()
      data class Range(val key: String, val start: Int, val end: Int)
      val ranges = ArrayList<Range>()
      while (true) {
        val tokenType = highlightingLexer.tokenType ?: break
        val tokenHighlights = rHighlighter.getTokenHighlights(tokenType)
        if (tokenHighlights.size == 1) {
          val textAttributesKey = tokenHighlights[0]
          val externalName = textAttributesKey.externalName
          if (!tags.containsKey(externalName)) {
            tags[externalName] = textAttributesKey
          }
          ranges.add(Range(externalName, highlightingLexer.tokenStart, highlightingLexer.tokenEnd))
        }
        highlightingLexer.advance()
      }
      var finalText = R_DEMO
      for (range in ranges.reversed()) {
        finalText = finalText.substring(0, range.start) + "<${range.key}>" + finalText.subSequence(range.start, range.end) + "</${range.key}>" + finalText.substring(range.end)
      }
      return finalText
    }

    private val R_MARKDOWN_DEMO = """
---
title: "Untitled"
author: user
date: 12/2/19
output: rmarkdown::html_vignette
---
       
<chunk>```{r}
${demoWithLexerHighlighting}
```
</chunk>
""".trimIndent()
  }
}
