// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.stubs;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.r.psi.api.RAssignmentStatement;

public class RAssignmentStubImpl extends StubBase<RAssignmentStatement> implements RAssignmentStub {
    private final String myName;
    private final boolean isFunction;
    private final boolean isTopLevel;
    private final boolean isRight;


    public RAssignmentStubImpl(@Nullable final String name,
                               @NotNull final StubElement parent,
                               @NotNull IStubElementType stubElementType,
                               boolean isFunctionDefinition,
                               boolean isTopLevel,
                               boolean isRight) {
        super(parent, stubElementType);
        this.myName = name;
        this.isFunction = isFunctionDefinition;
        this.isTopLevel = isTopLevel;
        this.isRight = isRight;
    }


    @Override
    public String getName() {
        return myName;
    }


    @Override
    public String toString() {
        return "RAssignmentStub(" + myName + ")";
    }


    @Override
    public boolean isFunctionDeclaration() {
        return isFunction;
    }


    @Override
    public boolean isPrimitiveFunctionDeclaration() {
        return false;
    }

    @Override
    public boolean isTopLevelAssignment() {
        return isTopLevel;
    }

    @Override
    public boolean isRight() {
        return isRight;
    }
}
