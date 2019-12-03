/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor

private typealias Colors = DefaultLanguageHighlighterColors

private val descriptors = ArrayList<AttributesDescriptor>()
val DESCRIPTORS: Array<AttributesDescriptor> by lazy { descriptors.toTypedArray().also { descriptors.clear() } }

val LINE_COMMENT         = create("R_LINE_COMMENT", Colors.LINE_COMMENT, "Line Comment")
val KEYWORD              = create("R_KEYWORD", Colors.KEYWORD, "Keyword")
val NUMBER               = create("R_NUMBER", Colors.NUMBER, "Number")
val STRING               = create("R_STRING", Colors.STRING, "String")

val OPERATION_SIGN       = create("R_OPERATION_SIGN", Colors.OPERATION_SIGN, "Braces and Operators//Operator sign")
val PARENTHESES          = create("R_PARENTHESES", Colors.PARENTHESES, "Braces and Operators//Parentheses")
val BRACKETS             = create("R_BRACKETS", Colors.BRACKETS, "Braces and Operators//Brackets")
val BRACES               = create("R_BRACES", Colors.BRACES, "Braces and Operators//Braces")
val COMMA                = create("R_COMMA", Colors.COMMA, "Braces and Operators//Comma")
val SEMICOLON            = create("R_SEMICOLON", Colors.SEMICOLON, "Braces and Operators//Semicolon")

val LOCAL_VARIABLE       = create("R_LOCAL_VARIABLE", Colors.LOCAL_VARIABLE, "Variables//Local Variable")
val FIELD                = create("R_FIELD", Colors.INSTANCE_FIELD, "Variables//Variable Access")
val CLOSURE              = create("R_CLOSURE", Colors.CLASS_NAME, "Variables//Closure Variable")

val NAMESPACE            = create("R_NAMESPACE", Colors.CLASS_NAME, "Namespace")

val PARAMETER            = create("R_PARAMETER", Colors.PARAMETER, "Parameter")
val NAMED_ARGUMENT       = create("R_NAMED_ARGUMENT", Colors.IDENTIFIER, "Named Argument")

val FUNCTION_CALL        = create("R_FUNCTION_CALL", Colors.STATIC_METHOD, "Functions//Function call")
val FUNCTION_DECLARATION = create("R_FUNCTION_DECLARATION", Colors.FUNCTION_DECLARATION, "Functions//Function declaration")

val BAD_CHARACTER        = create("R_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER, "Bad character")

val RMARKDOWN_CHUNK = TextAttributesKey.createTextAttributesKey("RMARKDOWN_CHUNK", HighlighterColors.TEXT)

private fun create(externalName: String, fallback: TextAttributesKey, displayName: String): TextAttributesKey =
  TextAttributesKey.createTextAttributesKey(externalName, fallback).also { descriptors.add(AttributesDescriptor(displayName, it)) }
