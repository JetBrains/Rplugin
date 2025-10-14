// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveVisitor;
import com.intellij.r.psi.psi.api.RVisitor;
import org.jetbrains.annotations.NotNull;

public class RRecursiveElementVisitor extends RVisitor implements PsiRecursiveVisitor {
    @Override
    public void visitElement(@NotNull PsiElement element) {
        element.acceptChildren(this);
    }
}
