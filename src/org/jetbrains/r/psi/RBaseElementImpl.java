// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.RLanguage;
import org.jetbrains.r.psi.api.RPsiElement;

public class RBaseElementImpl<T extends StubElement> extends StubBasedPsiElementBase<T> implements RPsiElement {
    public RBaseElementImpl(@NotNull final ASTNode node) {
        super(node);
    }


    public RBaseElementImpl(@NotNull final T stub, @NotNull final IStubElementType nodeType) {
        super(stub, nodeType);
    }


    @NotNull
    @Override
    public Language getLanguage() {
        return RLanguage.INSTANCE;
    }


    @Override
    public String toString() {
        return getNode().getElementType().toString();
    }


    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
      super.accept(visitor);
    }
}
