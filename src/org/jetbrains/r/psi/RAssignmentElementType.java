// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.*;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.psi.api.RAssignmentStatement;
import org.jetbrains.r.psi.api.RFile;
import org.jetbrains.r.psi.api.RFunctionExpression;
import org.jetbrains.r.psi.api.RPsiElement;
import org.jetbrains.r.psi.impl.RAssignmentStatementImpl;
import org.jetbrains.r.psi.stubs.*;

import java.io.IOException;

public class RAssignmentElementType extends RStubElementType<RAssignmentStub, RAssignmentStatement> {
    RAssignmentElementType(@NotNull final String debugName) {
        super(debugName);
    }


    @Override
    public PsiElement createElement(@NotNull final ASTNode node) {
        return new RAssignmentStatementImpl(node);
    }


    @Override
    public RAssignmentStatement createPsi(@NotNull final RAssignmentStub stub) {
        return new RAssignmentStatementImpl(stub, this);
    }


    @NotNull
    @Override
    public RAssignmentStub createStub(@NotNull RAssignmentStatement psi, StubElement parentStub) {
        final String name = psi.getName();
        final RPsiElement value = psi.getAssignedValue();
        boolean isTopLevelAssign = value != null && value.getParent() != null && value.getParent().getParent() != null && value.getParent().getParent() instanceof RFile;

        return new RAssignmentStubImpl(name, parentStub, this,
                                       value instanceof RFunctionExpression, isTopLevelAssign,
                                       psi.isRight());
    }

    @Override
    public void serialize(@NotNull final RAssignmentStub stub, @NotNull final StubOutputStream dataStream)
            throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.isFunctionDeclaration());
        dataStream.writeBoolean(stub.isTopLevelAssignment());
        dataStream.writeBoolean(stub.isRight());
    }


    @Override
    @NotNull
    public RAssignmentStub deserialize(@NotNull final StubInputStream dataStream, final StubElement parentStub) throws IOException {
        String name = StringRef.toString(dataStream.readName());
        final boolean isFunctionDefinition = dataStream.readBoolean();
        final boolean isTopLevel = dataStream.readBoolean();
        final boolean isRight = dataStream.readBoolean();
        return new RAssignmentStubImpl(name, parentStub, this, isFunctionDefinition, isTopLevel, isRight);
    }


    @Override
    public void indexStub(@NotNull final RAssignmentStub stub, @NotNull final IndexSink sink) {
        final String name = stub.getName();
        if (name != null && stub.getParentStub() instanceof PsiFileStub && stub.isTopLevelAssignment()) {
            RAssignmentNameIndex.sink(sink, name);
            RAssignmentCompletionIndex.Companion.sink(sink, "");
        }
    }
}
