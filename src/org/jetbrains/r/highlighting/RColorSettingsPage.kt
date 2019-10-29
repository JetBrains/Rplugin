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

class RColorSettingsPage : ColorSettingsPage {

  override fun getIcon(): Icon? = R_LOGO_16

  override fun getHighlighter(): SyntaxHighlighter = RHighlighter()

  override fun getDemoText(): String = R_DEMO

  override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = TAGS

  override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

  override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

  override fun getDisplayName(): String = "R"

  companion object {
    /**
     * To be sure that annotator output is consistent with R highlighting demo
     * we use the same input in the annotator test and in R highlighting demo.
     */
    internal val R_DEMO_FOR_TESTS = """
# function declaration 
<info descr="R_FUNCTION_DECLARATION">global_function</info> <- function(<info descr="R_PARAMETER">regular</info>, <info descr="R_PARAMETER">named</info> = "Hello World!") {
  # inner function declaration
  <info descr="R_FUNCTION_DECLARATION">inner_function</info> <- function() {
     <info descr="R_LOCAL_VARIABLE">closure_usage</info> <- <info descr="R_CLOSURE">regular</info>
     <info descr="R_LOCAL_VARIABLE">closure_usage</info> + 1 
  }
   
  <info descr="R_FUNCTION_CALL">print</info>(<info descr="R_PARAMETER">named</info>)
  <info descr="R_PARAMETER">regular</info> + <info descr="R_FUNCTION_CALL">inner_function</info>()
}

# function call
<info descr="R_FUNCTION_CALL">global_function</info>(2, <info descr="R_NAMED_ARGUMENT">named</info> = 'Hello World!')

# namespace access
<info descr="R_FUNCTION_CALL">print</info>(<info descr="R_NAMESPACE">datasets</info>::cars[1, 2])
    """.trimIndent()
  }
  private val R_DEMO = R_DEMO_FOR_TESTS.replace("<info descr=\"([^<>\"]*)\">([^<>\"]*)</info>".toRegex(), "<$1>$2</$1>")
  private val TAGS = DESCRIPTORS.map { it.key }.filter { R_DEMO.contains(it.externalName) }.map { it.externalName to it }.toMap()
}
