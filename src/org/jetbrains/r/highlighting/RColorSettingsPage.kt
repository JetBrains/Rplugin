// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova

/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.jetbrains.r.*

import javax.swing.*

/**
 * RColorSettingsPage implementation
 * Created on 7/23/14.
 *
 * @author HongKee Moon
 */
class RColorSettingsPage : ColorSettingsPage {

  override fun getIcon(): Icon? {
    return R_LOGO_16
  }

  override fun getHighlighter(): SyntaxHighlighter {
    return RSyntaxHighlighter()
  }

  override fun getDemoText(): String {
    return SAMPLE_R_SCRIPT
  }

  override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? {
    return null
  }

  override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
    return DESCRIPTORS
  }

  override fun getColorDescriptors(): Array<ColorDescriptor> {
    return ColorDescriptor.EMPTY_ARRAY
  }

  override fun getDisplayName(): String {
    return "R"
  }

  companion object {
    /**
     * The path to the sample .R file
     */
    private val SAMPLE_R_SCRIPT = """
      
    """.trimIndent()

    /**
     * The sample .R document shown in the colors settings dialog
     */
    private val DESCRIPTORS = arrayOf(AttributesDescriptor("Comment", RHighlighterColors.LINE_COMMENT),
                                      AttributesDescriptor("Keyword", RHighlighterColors.KEYWORD),
                                      AttributesDescriptor("Parenthesis", RHighlighterColors.PARENTHESES),
                                      AttributesDescriptor("Braces", RHighlighterColors.BRACES),
                                      AttributesDescriptor("Brackets", RHighlighterColors.BRACKETS),
                                      AttributesDescriptor("Number", RHighlighterColors.NUMBER),
                                      AttributesDescriptor("String ...", RHighlighterColors.STRING),
                                      AttributesDescriptor("Function Call", RHighlighterColors.FUNCTION_CALL),
                                      AttributesDescriptor("Namespace", RHighlighterColors.NAMESPACE),
                                      AttributesDescriptor("Parameter", RHighlighterColors.PARAMETER),
                                      AttributesDescriptor("Local variable", RHighlighterColors.LOCAL_VARIABLE),
                                      AttributesDescriptor("Global variable", RHighlighterColors.GLOBAL_VARIABLE),
                                      AttributesDescriptor("Closure", RHighlighterColors.CLOSURE),
                                      AttributesDescriptor("Named argument", RHighlighterColors.NAMED_ARGUMENT))
  }
}
