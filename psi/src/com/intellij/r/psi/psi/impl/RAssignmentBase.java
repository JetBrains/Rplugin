// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.r.psi.psi.RBaseElementImpl;
import com.intellij.r.psi.psi.api.RExpression;
import com.intellij.r.psi.psi.references.RReferenceBase;
import com.intellij.r.psi.psi.stubs.RAssignmentStub;
import org.jetbrains.annotations.NotNull;

public abstract class RAssignmentBase extends RBaseElementImpl<RAssignmentStub>
                                      implements RExpression, PsiNameIdentifierOwner {

    RAssignmentBase(@NotNull ASTNode node) {
        super(node);
    }

    RAssignmentBase(@NotNull RAssignmentStub stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public RReferenceBase<?> getReference() {
        return null;
    }
}
