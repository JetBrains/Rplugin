// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova


/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.highlighting;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.lexer.RLexer;
import org.jetbrains.r.parsing.RElementTypes;
import org.jetbrains.r.parsing.RParserDefinition;

import java.util.HashMap;
import java.util.Map;


/**
 * Defines R token highlighting and formatting.
 * <p>
 * See https://confluence.jetbrains.com/display/IntelliJIDEA/Syntax+Highlighter+and+Color+Settings+Page
 *
 * @author Holger Brandl
 * @author HongKee Moon
 */
public class RSyntaxHighlighter extends SyntaxHighlighterBase {

    private static final TokenSet lineCommentSet = TokenSet.create(RParserDefinition.END_OF_LINE_COMMENT);
    private static final TokenSet parenthesisSet = TokenSet.create(RElementTypes.R_LPAR, RElementTypes.R_RPAR);
    private static final TokenSet curlySet = TokenSet.create(RElementTypes.R_LBRACE, RElementTypes.R_RBRACE);
    private static final TokenSet bracketSet = TokenSet.create(RElementTypes.R_LBRACKET, RElementTypes.R_RBRACKET);
    private static final TokenSet string2Set = TokenSet.create(RElementTypes.R_STRING);
    private static final TokenSet numberSet = TokenSet.create(RElementTypes.R_NUMERIC);

    //custom high-lighting definitions
    private static final TextAttributes SHEBANG_ATTRIB = DefaultLanguageHighlighterColors.LINE_COMMENT.getDefaultAttributes().clone();

    private static final Map<IElementType, TextAttributesKey> attributes;

    private static final TokenSet keywords = TokenSet.create(RElementTypes.R_ELSE, RElementTypes.R_FOR, RElementTypes.R_FUNCTION, RElementTypes.R_IF, RElementTypes.R_WHILE);

    static {
        //setup default attribute formatting
        SHEBANG_ATTRIB.setFontType(SimpleTextAttributes.STYLE_BOLD);

        attributes = new HashMap<>();

        fillMap(attributes, lineCommentSet, RHighlighterColors.COMMA);
        fillMap(attributes, keywords, RHighlighterColors.KEYWORD);
        fillMap(attributes, parenthesisSet, RHighlighterColors.PARENTHESES);
        fillMap(attributes, curlySet, RHighlighterColors.BRACES);
        fillMap(attributes, bracketSet, RHighlighterColors.BRACKETS);
        fillMap(attributes, string2Set, RHighlighterColors.STRING);
        fillMap(attributes, numberSet, RHighlighterColors.NUMBER);
    }

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new RLexer();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(final IElementType tokenType) {
        return pack(attributes.get(tokenType));
    }
}


