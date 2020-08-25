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
import icons.RIcons
import org.jetbrains.annotations.NonNls
import org.jetbrains.r.RLanguage
import javax.swing.Icon

class RColorSettingsPage : RainbowColorSettingsPage {

  override fun getIcon(): Icon? = RIcons.R_logo_16

  override fun isRainbowType(type: TextAttributesKey?): Boolean = type == LOCAL_VARIABLE || type == PARAMETER || type == CLOSURE

  override fun getHighlighter(): SyntaxHighlighter = RHighlighter()

  override fun getDemoText(): String = R_DEMO

  override fun getLanguage(): Language = RLanguage.INSTANCE

  override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = TAGS

  override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

  override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

  @NonNls
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

<info descr="R_LOCAL_VARIABLE">cat</info> = <info descr="R_FUNCTION_CALL">list</info>(<info descr="R_NAMED_ARGUMENT">name</info> = "Smudge", <info descr="R_NAMED_ARGUMENT">breed</info> = "Maine Coon")
#variable acсess
<info descr="R_FUNCTION_CALL">print</info>(<info descr="R_LOCAL_VARIABLE">cat</info>${'$'}<info descr="R_FIELD">breed</info>)

# namespace access
<info descr="R_FUNCTION_CALL">print</info>(<info descr="R_NAMESPACE">datasets</info>::cars[1, 2])
    """.trimIndent()
  internal val R_DEMO = R_DEMO_FOR_TESTS.replace("<info descr=\"([^<>\"]*)\">([^<>\"]*)</info>".toRegex(), "<$1>$2</$1>")
  internal val TAGS = DESCRIPTORS.map { it.key }.filter { R_DEMO.contains(it.externalName) }.map { it.externalName to it }.toMap()
  }
}
