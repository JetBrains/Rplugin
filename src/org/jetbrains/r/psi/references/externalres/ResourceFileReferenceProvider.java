// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.references.externalres;

import com.intellij.openapi.paths.PathReferenceManager;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.psi.api.RFile;
import org.jetbrains.r.psi.impl.RStringLiteralExpressionImpl;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.PlatformPatterns.psiFile;

@Deprecated // replaced with ResourceScriptReferenceContributor
public class ResourceFileReferenceProvider extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        // for additional patterns see https://intellij-support.jetbrains.com/hc/en-us/community/posts/206114249-How-to-complete-string-literal-expressions-
        registrar.registerReferenceProvider(psiElement(RStringLiteralExpressionImpl.class).inFile(psiFile(RFile.class)),
                new LinkDestinationReferenceProvider());
    }


    private static class LinkDestinationReferenceProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
            return PathReferenceManager.getInstance().createReferences(element, false, false, true);
        }
    }
}
