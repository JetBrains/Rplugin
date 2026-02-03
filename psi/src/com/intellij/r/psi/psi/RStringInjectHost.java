// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.r.psi.RLanguage;
import com.intellij.r.psi.parsing.RElementTypes;
import com.intellij.r.psi.psi.api.RStringLiteralExpression;
import com.intellij.r.psi.psi.references.RReferenceBase;
import org.jetbrains.annotations.NotNull;


/**
 * @author gregsh
 * @author brandl
 */
public abstract class RStringInjectHost extends ASTWrapperPsiElement implements RStringLiteralExpression, PsiLanguageInjectionHost {

    public RStringInjectHost(ASTNode node) {
        super(node);
    }


    @Override
    public boolean isValidHost() {
        return true;
    }


    /**
     * note: this method is called if incjected snippet is edited in split pane view.
     */
    @Override
    public RStringInjectHost updateText(final @NotNull String text) {
        final RStringLiteralExpression expression = createExpressionFromText(getProject(), text);
        assert expression != null : text + "-->" + expression;
        return (RStringInjectHost) this.replace(expression);
    }


    @Override
    public RReferenceBase<?> getReference() {
        return null;
    }

    // just needed to for compatibility with exsting test-results. Should be removed and results should be updated
    @Override
    public String toString() {
        return RElementTypes.R_STRING_LITERAL_EXPRESSION.toString();
    }
    // provide path completion. see ResourceFileRefernceProvider
    @Override
    public @NotNull PsiReference[] getReferences() {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this);
    }


    public static RStringInjectHost createExpressionFromText(Project project, String text) {
//        PsiFile fromText = PsiFileFactory.getInstance(project).createFileFromText("a.R", "\"" + text + "\";");
      PsiFile fromText = PsiFileFactory.getInstance(project).createFileFromText("a.R", RLanguage.INSTANCE, text + ";");
        if ((fromText.getFirstChild()) != null) {
            return (RStringInjectHost) fromText.getFirstChild();
        }
        return null;
    }


    @Override
    public @NotNull LiteralTextEscaper<? extends PsiLanguageInjectionHost> createLiteralTextEscaper() {
        return new RStringLiteralEscaper(this);
    }
}
