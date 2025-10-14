/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.r.psi.RBundle
import org.jetbrains.annotations.Nls

private typealias Colors = DefaultLanguageHighlighterColors

private val descriptors = ArrayList<AttributesDescriptor>()
val DESCRIPTORS: Array<AttributesDescriptor> by lazy { descriptors.toTypedArray().also { descriptors.clear() } }

val LINE_COMMENT         = create("R_LINE_COMMENT", Colors.LINE_COMMENT, RBundle.message("r.colors.line.comment"))
val DOC_COMMENT          = create("R_DOCUMENT_COMMENT", Colors.DOC_COMMENT, RBundle.message("r.colors.document.comment"))
val KEYWORD              = create("R_KEYWORD", Colors.KEYWORD, RBundle.message("r.colors.keyword"))
val NUMBER               = create("R_NUMBER", Colors.NUMBER, RBundle.message("r.colors.number"))
val STRING               = create("R_STRING", Colors.STRING, RBundle.message("r.colors.string"))

val OPERATION_SIGN       = create("R_OPERATION_SIGN", Colors.OPERATION_SIGN, RBundle.message("r.colors.sign"))
val PARENTHESES          = create("R_PARENTHESES", Colors.PARENTHESES, RBundle.message("r.colors.parentheses"))
val BRACKETS             = create("R_BRACKETS", Colors.BRACKETS, RBundle.message("r.colors.brackets"))
val BRACES               = create("R_BRACES", Colors.BRACES, RBundle.message("r.colors.braces"))
val COMMA                = create("R_COMMA", Colors.COMMA, RBundle.message("r.colors.comma"))
val SEMICOLON            = create("R_SEMICOLON", Colors.SEMICOLON, RBundle.message("r.colors.semicolon"))

val LOCAL_VARIABLE       = create("R_LOCAL_VARIABLE", Colors.LOCAL_VARIABLE, RBundle.message("r.colors.local.variable"))
val FIELD                = create("R_FIELD", Colors.INSTANCE_FIELD, RBundle.message("r.colors.variable.access"))
val CLOSURE              = create("R_CLOSURE", Colors.PARAMETER, RBundle.message("r.colors.closure.variable"))

val NAMESPACE            = create("R_NAMESPACE", Colors.CLASS_NAME, RBundle.message("r.colors.namespace"))

val PARAMETER            = create("R_PARAMETER", Colors.PARAMETER, RBundle.message("r.colors.parameter"))
val NAMED_ARGUMENT       = create("R_NAMED_ARGUMENT", Colors.IDENTIFIER, RBundle.message("r.colors.named.argument"))

val FUNCTION_CALL        = create("R_FUNCTION_CALL", Colors.FUNCTION_CALL, RBundle.message("r.colors.function.call"))
val FUNCTION_DECLARATION = create("R_FUNCTION_DECLARATION", Colors.FUNCTION_DECLARATION, RBundle.message("r.colors.function.declaration"))

val BAD_CHARACTER        = create("R_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER, RBundle.message("r.colors.bad.character"))

val RMARKDOWN_CHUNK = TextAttributesKey.createTextAttributesKey("RMARKDOWN_CHUNK", HighlighterColors.TEXT)

private fun create(externalName: String, fallback: TextAttributesKey, displayName: @Nls String): TextAttributesKey =
  TextAttributesKey.createTextAttributesKey(externalName, fallback).also { descriptors.add(AttributesDescriptor(displayName, it)) }
