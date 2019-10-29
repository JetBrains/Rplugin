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
    return RHighlighter()
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
    internal val SAMPLE_R_SCRIPT = """
      # global variable
      global_variable <- 1
      
      # function declaration 
      global_function <- function(regular, named = "Hello World!") {
        # inner function declaration
        inner_function <- function() {
           closure_usage <- regular
           closure_usage + 1
        }
         
        print(named)
        regular + inner_function()
      }
      
      # function call
      global_function(2, named = 'Hello World!')
      
      # namespace access
      print(datasets::cars[1, 2])
      
      # bad character
      x <- 1 + 2 + 4 5
    """.trimIndent()
  }
}
