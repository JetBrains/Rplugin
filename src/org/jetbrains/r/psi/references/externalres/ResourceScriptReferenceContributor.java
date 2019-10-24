// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.references.externalres;

import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.psi.api.RCallExpression;
import org.jetbrains.r.psi.api.RIdentifierExpression;
import org.jetbrains.r.psi.api.RStringLiteralExpression;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class ResourceScriptReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(SOURCE_URL_PATTERN, new ResourceScriptReferenceProvider());
    }


    public static final PsiElementPattern.Capture<RStringLiteralExpression> SOURCE_URL_PATTERN =
            psiElement(RStringLiteralExpression.class).
                    withSuperParent(2, psiElement(RCallExpression.class).withChild(psiElement(RIdentifierExpression.class).withText(StandardPatterns.string().oneOf("source_url", "devtools::source_url", "source"))));
}