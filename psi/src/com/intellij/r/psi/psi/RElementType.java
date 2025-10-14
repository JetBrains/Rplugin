// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.r.psi.RLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;

public class RElementType extends IElementType {
    protected Class<? extends PsiElement> myPsiElementClass;
    private Constructor<? extends PsiElement> myConstructor;
    private static final Class[] PARAMETER_TYPES = new Class[]{ASTNode.class};


    public RElementType(final @NotNull @NonNls String debugName) {
        super(debugName, RLanguage.INSTANCE);
    }


    public RElementType(final @NotNull @NonNls String debugName, final @NotNull Class<? extends PsiElement> psiElementClass) {
        super(debugName, RLanguage.INSTANCE);
        myPsiElementClass = psiElementClass;
    }


    public @NotNull PsiElement createElement(final @NotNull ASTNode node) {
        if (myPsiElementClass == null) {
            throw new IllegalStateException("Cannot create an element for " + node.getElementType() + " without element class");
        }
        try {
            if (myConstructor == null) {
                myConstructor = myPsiElementClass.getConstructor(PARAMETER_TYPES);
            }
            return myConstructor.newInstance(node);
        } catch (Exception e) {
            throw new IllegalStateException("No necessary constructor for " + node.getElementType(), e);
        }
    }
}
