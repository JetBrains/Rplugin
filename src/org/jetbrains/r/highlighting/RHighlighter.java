// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.highlighting;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.lexer.RLexer;
import org.jetbrains.r.parsing.RElementTypes;
import org.jetbrains.r.parsing.RParserDefinition;
import org.jetbrains.r.psi.RPsiUtil;

import java.util.HashMap;
import java.util.Map;

public class RHighlighter extends SyntaxHighlighterBase {
    private static final Map<IElementType, TextAttributesKey> ATTRIBUTES = new HashMap<IElementType, TextAttributesKey>();

    static {
        fillMap(ATTRIBUTES, RPsiUtil.INSTANCE.getRESERVED_WORDS(), RHighlighterColors.KEYWORD);
        fillMap(ATTRIBUTES, RPsiUtil.INSTANCE.getOPERATORS(), RHighlighterColors.OPERATION_SIGN);

        ATTRIBUTES.put(RElementTypes.R_STRING, RHighlighterColors.STRING);
        ATTRIBUTES.put(RElementTypes.R_NUMERIC, RHighlighterColors.NUMBER);
        ATTRIBUTES.put(RElementTypes.R_COMPLEX, RHighlighterColors.NUMBER);
        ATTRIBUTES.put(RElementTypes.R_INTEGER, RHighlighterColors.NUMBER);


        ATTRIBUTES.put(RElementTypes.R_LPAR, RHighlighterColors.PARENTHESES);
        ATTRIBUTES.put(RElementTypes.R_RPAR, RHighlighterColors.PARENTHESES);

        ATTRIBUTES.put(RElementTypes.R_LBRACE, RHighlighterColors.BRACES);
        ATTRIBUTES.put(RElementTypes.R_RBRACE, RHighlighterColors.BRACES);

        ATTRIBUTES.put(RElementTypes.R_LBRACKET, RHighlighterColors.BRACKETS);
        ATTRIBUTES.put(RElementTypes.R_LDBRACKET, RHighlighterColors.BRACKETS);
        ATTRIBUTES.put(RElementTypes.R_RBRACKET, RHighlighterColors.BRACKETS);
        ATTRIBUTES.put(RElementTypes.R_RDBRACKET, RHighlighterColors.BRACKETS);

        ATTRIBUTES.put(RElementTypes.R_COMMA, RHighlighterColors.COMMA);
        ATTRIBUTES.put(RElementTypes.R_SEMI, RHighlighterColors.SEMICOLON);

        ATTRIBUTES.put(RParserDefinition.END_OF_LINE_COMMENT, RHighlighterColors.LINE_COMMENT);

        ATTRIBUTES.put(RParserDefinition.BAD_CHARACTER, RHighlighterColors.BAD_CHARACTER);
    }

    @Override
    @NotNull
    public Lexer getHighlightingLexer() {
        return new RLexer();
    }


    @Override
    @NotNull
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        return pack(ATTRIBUTES.get(tokenType));
    }
}
