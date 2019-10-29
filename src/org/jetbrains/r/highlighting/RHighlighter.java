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
        fillMap(ATTRIBUTES, RPsiUtil.INSTANCE.getRESERVED_WORDS(), RHighlighterColorsKt.getKEYWORD());
        fillMap(ATTRIBUTES, RPsiUtil.INSTANCE.getOPERATORS(), RHighlighterColorsKt.getOPERATION_SIGN());

        ATTRIBUTES.put(RElementTypes.R_STRING, RHighlighterColorsKt.getSTRING());
        ATTRIBUTES.put(RElementTypes.R_NUMERIC, RHighlighterColorsKt.getNUMBER());
        ATTRIBUTES.put(RElementTypes.R_COMPLEX, RHighlighterColorsKt.getNUMBER());
        ATTRIBUTES.put(RElementTypes.R_INTEGER, RHighlighterColorsKt.getNUMBER());


        ATTRIBUTES.put(RElementTypes.R_LPAR, RHighlighterColorsKt.getPARENTHESES());
        ATTRIBUTES.put(RElementTypes.R_RPAR, RHighlighterColorsKt.getPARENTHESES());

        ATTRIBUTES.put(RElementTypes.R_LBRACE, RHighlighterColorsKt.getBRACES());
        ATTRIBUTES.put(RElementTypes.R_RBRACE, RHighlighterColorsKt.getBRACES());

        ATTRIBUTES.put(RElementTypes.R_LBRACKET, RHighlighterColorsKt.getBRACKETS());
        ATTRIBUTES.put(RElementTypes.R_LDBRACKET, RHighlighterColorsKt.getBRACKETS());
        ATTRIBUTES.put(RElementTypes.R_RBRACKET, RHighlighterColorsKt.getBRACKETS());
        ATTRIBUTES.put(RElementTypes.R_RDBRACKET, RHighlighterColorsKt.getBRACKETS());

        ATTRIBUTES.put(RElementTypes.R_COMMA, RHighlighterColorsKt.getCOMMA());
        ATTRIBUTES.put(RElementTypes.R_SEMI, RHighlighterColorsKt.getSEMICOLON());

        ATTRIBUTES.put(RParserDefinition.END_OF_LINE_COMMENT, RHighlighterColorsKt.getLINE_COMMENT());

        ATTRIBUTES.put(RParserDefinition.BAD_CHARACTER, RHighlighterColorsKt.getBAD_CHARACTER());
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
