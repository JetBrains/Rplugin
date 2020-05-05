// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.parsing;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.lexer.RLexer;
import org.jetbrains.r.psi.RElementType;
import org.jetbrains.r.psi.RFileElementType;
import org.jetbrains.r.psi.RFileImpl;

public class RParserDefinition implements ParserDefinition {
    private final TokenSet myCommentTokens;
    private final TokenSet myStringLiteralTokens;

    public static RFileElementType FILE = new RFileElementType();
    public static IElementType END_OF_LINE_COMMENT = new RElementType("END_OF_LINE_COMMENT");
    public static IElementType ROXYGEN_COMMENT = new RElementType("ROXYGEN_COMMENT");
    public static IElementType BAD_CHARACTER = new RElementType("BAD_CHARACTER");

    public RParserDefinition() {
        myCommentTokens = TokenSet.create(END_OF_LINE_COMMENT, ROXYGEN_COMMENT);
        myStringLiteralTokens = TokenSet.create(RElementTypes.R_STRING);
    }


    @Override
    @NotNull
    public Lexer createLexer(Project project) {
        return new RLexer();
    }


    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }


    //@Override
    //@NotNull
    //public TokenSet getWhitespaceTokens() {
    //    return myWhitespaceTokens;
    //}


    @Override
    @NotNull
    public TokenSet getCommentTokens() {
        return myCommentTokens;
    }


    @Override
    @NotNull
    public TokenSet getStringLiteralElements() {
        return myStringLiteralTokens;
    }


    @Override
    @NotNull
    public PsiParser createParser(Project project) {
        return new RParser();
    }


    @Override
    @NotNull
    public PsiElement createElement(@NotNull ASTNode node) {
        return RElementTypes.Factory.createElement(node);
    }


    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new RFileImpl(viewProvider);
    }


    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }
}
