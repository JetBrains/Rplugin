// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring;

import com.intellij.lang.refactoring.InlineHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Holger Brandl
 */
public class RInlineHandler implements InlineHandler {

    @Nullable
    @Override
    public Settings prepareInlineElement(@NotNull PsiElement psiElement, @Nullable Editor editor, boolean b) {
        return Settings.CANNOT_INLINE_SETTINGS;
    }


    @Override
    public void removeDefinition(@NotNull PsiElement psiElement, @NotNull Settings settings) {
        psiElement.delete();
    }


    @Nullable
    @Override
    public Inliner createInliner(@NotNull PsiElement psiElement, @NotNull Settings settings) {
//        if (element instanceof GrVariable) {
//            return new GrVariableInliner((GrVariable)element, settings);
//        }
//        if (element instanceof GrMethod) {
//            return new GroovyMethodInliner((GrMethod)element);
//        }
        return new Inliner() {

            @Override
            public MultiMap<PsiElement, String> getConflicts(@NotNull PsiReference psiReference, @NotNull PsiElement psiElement) {
                return MultiMap.empty();
            }


            @Override
            public void inlineUsage(@NotNull UsageInfo usageInfo, @NotNull PsiElement psiElement) {
                System.out.println("sdf");
            }
        };
    }
}
