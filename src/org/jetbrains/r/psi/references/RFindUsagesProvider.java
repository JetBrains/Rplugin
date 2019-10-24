// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.references;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.r.lexer.RLexer;
import org.jetbrains.r.parsing.RElementTypes;
import org.jetbrains.r.parsing.RParserDefinition;
import org.jetbrains.r.psi.api.*;

public class RFindUsagesProvider implements FindUsagesProvider {
    @Nullable
    @Override
    public WordsScanner getWordsScanner() {
        return new DefaultWordsScanner(new RLexer(), TokenSet.create(RElementTypes.R_IDENTIFIER),
                TokenSet.create(RParserDefinition.END_OF_LINE_COMMENT),
                TokenSet.create(RElementTypes.R_STRING_LITERAL_EXPRESSION));
    }


    @Override
    public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
//        isLibraryFile(psiElement.getContainingFile())
        return psiElement instanceof PsiNamedElement || psiElement instanceof RIdentifierExpression;
    }


    @Nullable
    @Override
    public String getHelpId(@NotNull PsiElement psiElement) {
        return null;
    }


    @NotNull
    @Override
    public String getType(@NotNull PsiElement element) {
        if (element instanceof RAssignmentStatement) {
            RPsiElement assignedValue = ((RAssignmentStatement) element).getAssignedValue();
            if (assignedValue instanceof RFunctionExpression) {
                return "function";
            }
        }

        if (element instanceof RParameter) return "function parameter";

        return "variable";
    }


    @NotNull
    @Override
    public String getDescriptiveName(@NotNull PsiElement element) {
        if (element instanceof RAssignmentStatement) {
            PsiElement assignee = ((RAssignmentStatement) element).getAssignee();
            return assignee.getText();
        }

        // this will be used e.g. when renaming function parameters
        return element.getText();
    }


    @NotNull
    @Override
    public String getNodeText(@NotNull PsiElement element, boolean useFullName) {
        return element.getText();
    }
}
