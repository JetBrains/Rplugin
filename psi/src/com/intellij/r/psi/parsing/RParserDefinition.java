// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.parsing;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.r.psi.lexer.RLexer;
import com.intellij.r.psi.psi.RFileElementType;
import com.intellij.r.psi.psi.RFileImpl;
import org.jetbrains.annotations.NotNull;

public class RParserDefinition implements ParserDefinition {
    public static final RFileElementType FILE = new RFileElementType();

    public RParserDefinition() {
    }

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new RLexer();
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    //@Override
    //@NotNull
    //public TokenSet getWhitespaceTokens() {
    //    return myWhitespaceTokens;
    //}

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return RTokenTypes.COMMENTS;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return RTokenTypes.STRINGS;
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        return new RParser();
    }

    @Override
    public @NotNull PsiElement createElement(@NotNull ASTNode node) {
        return RElementTypes.Factory.createElement(node);
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new RFileImpl(viewProvider);
    }

    @Override
    public @NotNull SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }
}
