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
        fillMap(ATTRIBUTES, RPsiUtil.INSTANCE.getRESERVED_WORDS(), RHighlighterColors.Companion.getKEYWORD());
        fillMap(ATTRIBUTES, RPsiUtil.INSTANCE.getOPERATORS(), RHighlighterColors.Companion.getOPERATION_SIGN());

        ATTRIBUTES.put(RElementTypes.R_STRING, RHighlighterColors.Companion.getSTRING());
        ATTRIBUTES.put(RElementTypes.R_NUMERIC, RHighlighterColors.Companion.getNUMBER());
        ATTRIBUTES.put(RElementTypes.R_COMPLEX, RHighlighterColors.Companion.getNUMBER());
        ATTRIBUTES.put(RElementTypes.R_INTEGER, RHighlighterColors.Companion.getNUMBER());


        ATTRIBUTES.put(RElementTypes.R_LPAR, RHighlighterColors.Companion.getPARENTHESES());
        ATTRIBUTES.put(RElementTypes.R_RPAR, RHighlighterColors.Companion.getPARENTHESES());

        ATTRIBUTES.put(RElementTypes.R_LBRACE, RHighlighterColors.Companion.getBRACES());
        ATTRIBUTES.put(RElementTypes.R_RBRACE, RHighlighterColors.Companion.getBRACES());

        ATTRIBUTES.put(RElementTypes.R_LBRACKET, RHighlighterColors.Companion.getBRACKETS());
        ATTRIBUTES.put(RElementTypes.R_LDBRACKET, RHighlighterColors.Companion.getBRACKETS());
        ATTRIBUTES.put(RElementTypes.R_RBRACKET, RHighlighterColors.Companion.getBRACKETS());
        ATTRIBUTES.put(RElementTypes.R_RDBRACKET, RHighlighterColors.Companion.getBRACKETS());

        ATTRIBUTES.put(RElementTypes.R_COMMA, RHighlighterColors.Companion.getCOMMA());
        ATTRIBUTES.put(RElementTypes.R_SEMI, RHighlighterColors.Companion.getSEMICOLON());

        ATTRIBUTES.put(RParserDefinition.END_OF_LINE_COMMENT, RHighlighterColors.Companion.getLINE_COMMENT());

        ATTRIBUTES.put(RParserDefinition.BAD_CHARACTER, RHighlighterColors.Companion.getBAD_CHARACTER());
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
