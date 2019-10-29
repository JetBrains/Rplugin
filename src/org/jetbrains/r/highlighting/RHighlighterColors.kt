/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

interface RHighlighterColors {
  companion object {
    val R_KEYWORD = "R_KEYWORD"
    val R_LINE_COMMENT = "R_LINE_COMMENT"

    val R_NUMBER = "R_NUMBER"
    val R_STRING = "R_STRING"
    val R_OPERATION_SIGN = "R_OPERATION_SIGN"
    val R_PARENTHESES = "R_PARENTHESES"
    val R_BRACKETS = "R_BRACKETS"
    val R_BRACES = "R_BRACES"
    val R_COMMA = "R_COMMA"
    val R_SEMICOLON = "R_SEMICOLON"
    val R_BAD_CHARACTER = "R_BAD_CHARACTER"
    val R_FUNCTION_DECLARATION = "R_FUNCTION_DECLARATION"
    val R_FUNCTION_CALL = "R_FUNCTION_CALL"
    val R_LOCAL_VARIABLE = "R_LOCAL_VARIABLE"
    val R_GLOBAL_VARIABLE = "R_GLOBAL_VARIABLE"
    val R_NAMESPACE = "R_NAMESPACE"
    val R_PARAMETER = "R_PARAMETER"
    val R_NAMED_ARGUMENT = "R_NAMED_ARGUMENT"
    val R_CLOSURE = "R_CLOSURE"

    val LINE_COMMENT = createTextAttributesKey(R_LINE_COMMENT, DefaultLanguageHighlighterColors.LINE_COMMENT)
    val KEYWORD = createTextAttributesKey(R_KEYWORD, DefaultLanguageHighlighterColors.KEYWORD)
    val NUMBER = createTextAttributesKey(R_NUMBER, DefaultLanguageHighlighterColors.NUMBER)
    val STRING = createTextAttributesKey(R_STRING, DefaultLanguageHighlighterColors.STRING)
    val OPERATION_SIGN = createTextAttributesKey(R_OPERATION_SIGN, DefaultLanguageHighlighterColors.OPERATION_SIGN)
    val PARENTHESES = createTextAttributesKey(R_PARENTHESES, DefaultLanguageHighlighterColors.PARENTHESES)
    val BRACKETS = createTextAttributesKey(R_BRACKETS, DefaultLanguageHighlighterColors.BRACKETS)
    val BRACES = createTextAttributesKey(R_BRACES, DefaultLanguageHighlighterColors.BRACES)
    val COMMA = createTextAttributesKey(R_COMMA, DefaultLanguageHighlighterColors.COMMA)
    val SEMICOLON = createTextAttributesKey(R_SEMICOLON, DefaultLanguageHighlighterColors.SEMICOLON)
    val FUNCTION_CALL = createTextAttributesKey(R_FUNCTION_CALL, DefaultLanguageHighlighterColors.STATIC_METHOD)
    val LOCAL_VARIABLE = createTextAttributesKey(R_LOCAL_VARIABLE, DefaultLanguageHighlighterColors.LOCAL_VARIABLE)
    val GLOBAL_VARIABLE = createTextAttributesKey(R_GLOBAL_VARIABLE, DefaultLanguageHighlighterColors.GLOBAL_VARIABLE)
    val NAMESPACE = createTextAttributesKey(R_NAMESPACE, DefaultLanguageHighlighterColors.CLASS_NAME)
    val PARAMETER = createTextAttributesKey(R_PARAMETER, DefaultLanguageHighlighterColors.PARAMETER)
    val NAMED_ARGUMENT = createTextAttributesKey(R_NAMED_ARGUMENT, DefaultLanguageHighlighterColors.IDENTIFIER)
    val CLOSURE = createTextAttributesKey(R_CLOSURE, DefaultLanguageHighlighterColors.CLASS_NAME)
    val FUNCTION_DECLARATION = createTextAttributesKey(R_FUNCTION_DECLARATION, DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)

    val BAD_CHARACTER = createTextAttributesKey(R_BAD_CHARACTER, HighlighterColors.BAD_CHARACTER)
  }
}
