/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.highlighting;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public interface RHighlighterColors {
    String R_KEYWORD = "R_KEYWORD";
    String R_LINE_COMMENT = "R_LINE_COMMENT";

    String R_NUMBER = "R_NUMBER";
    String R_STRING = "R_STRING";
    String R_OPERATION_SIGN = "R_OPERATION_SIGN";
    String R_PARENTHESES = "R_PARENTHESES";
    String R_BRACKETS = "R_BRACKETS";
    String R_BRACES = "R_BRACES";
    String R_COMMA = "R_COMMA";
    String R_SEMICOLON = "R_SEMICOLON";
    String R_BAD_CHARACTER = "R_BAD_CHARACTER";
    String R_FUNCTION_DECLARATION = "R_FUNCTION_DECLARATION";
    String R_FUNCTION_CALL = "R_FUNCTION_CALL";
    String R_LOCAL_VARIABLE = "R_LOCAL_VARIABLE";
    String R_GLOBAL_VARIABLE = "R_GLOBAL_VARIABLE";
    String R_NAMESPACE = "R_NAMESPACE";
    String R_PARAMETER = "R_PARAMETER";
    String R_NAMED_ARGUMENT = "R_NAMED_ARGUMENT";
    String R_CLOSURE = "R_CLOSURE";

    TextAttributesKey LINE_COMMENT    = createTextAttributesKey(R_LINE_COMMENT,    DefaultLanguageHighlighterColors.LINE_COMMENT);
    TextAttributesKey KEYWORD         = createTextAttributesKey(R_KEYWORD,         DefaultLanguageHighlighterColors.KEYWORD);
    TextAttributesKey NUMBER          = createTextAttributesKey(R_NUMBER,          DefaultLanguageHighlighterColors.NUMBER);
    TextAttributesKey STRING          = createTextAttributesKey(R_STRING,          DefaultLanguageHighlighterColors.STRING);
    TextAttributesKey OPERATION_SIGN  = createTextAttributesKey(R_OPERATION_SIGN,  DefaultLanguageHighlighterColors.OPERATION_SIGN);
    TextAttributesKey PARENTHESES     = createTextAttributesKey(R_PARENTHESES,     DefaultLanguageHighlighterColors.PARENTHESES);
    TextAttributesKey BRACKETS        = createTextAttributesKey(R_BRACKETS,        DefaultLanguageHighlighterColors.BRACKETS);
    TextAttributesKey BRACES          = createTextAttributesKey(R_BRACES,          DefaultLanguageHighlighterColors.BRACES);
    TextAttributesKey COMMA           = createTextAttributesKey(R_COMMA,           DefaultLanguageHighlighterColors.COMMA);
    TextAttributesKey SEMICOLON       = createTextAttributesKey(R_SEMICOLON,       DefaultLanguageHighlighterColors.SEMICOLON);
    TextAttributesKey FUNCTION_CALL   = createTextAttributesKey(R_FUNCTION_CALL,   DefaultLanguageHighlighterColors.STATIC_METHOD);
    TextAttributesKey LOCAL_VARIABLE  = createTextAttributesKey(R_LOCAL_VARIABLE,  DefaultLanguageHighlighterColors.LOCAL_VARIABLE);
    TextAttributesKey GLOBAL_VARIABLE = createTextAttributesKey(R_GLOBAL_VARIABLE, DefaultLanguageHighlighterColors.GLOBAL_VARIABLE);
    TextAttributesKey NAMESPACE       = createTextAttributesKey(R_NAMESPACE,       DefaultLanguageHighlighterColors.CLASS_NAME);
    TextAttributesKey PARAMETER       = createTextAttributesKey(R_PARAMETER,       DefaultLanguageHighlighterColors.PARAMETER);
    TextAttributesKey NAMED_ARGUMENT  = createTextAttributesKey(R_NAMED_ARGUMENT,  DefaultLanguageHighlighterColors.IDENTIFIER);
    TextAttributesKey CLOSURE         = createTextAttributesKey(R_CLOSURE,         DefaultLanguageHighlighterColors.CLASS_NAME);
    TextAttributesKey FUNCTION_DECLARATION =
      createTextAttributesKey(R_FUNCTION_DECLARATION, DefaultLanguageHighlighterColors.FUNCTION_DECLARATION);

    TextAttributesKey BAD_CHARACTER  = createTextAttributesKey(R_BAD_CHARACTER,  HighlighterColors.BAD_CHARACTER);
}
