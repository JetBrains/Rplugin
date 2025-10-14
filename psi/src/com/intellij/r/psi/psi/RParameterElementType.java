/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.r.psi.psi.api.RParameter;
import com.intellij.r.psi.psi.impl.RParameterImpl;
import com.intellij.r.psi.psi.impl.RPsiImplUtil;
import com.intellij.r.psi.psi.stubs.RParameterStub;
import com.intellij.r.psi.psi.stubs.RParameterStubImpl;
import com.intellij.r.psi.psi.stubs.RStubElementType;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

public class RParameterElementType extends RStubElementType<RParameterStub, RParameter> {
  RParameterElementType(final @NotNull String debugName) {
    super(debugName);
  }

  @Override
  public PsiElement createElement(final @NotNull ASTNode node) {
    return new RParameterImpl(node);
  }

  @Override
  public RParameter createPsi(final @NotNull RParameterStub stub) {
    return new RParameterImpl(stub, this);
  }


  @Override
  public @NotNull RParameterStub createStub(@NotNull RParameter psi, StubElement parentStub) {
    final String name = psi.getName();
    return new RParameterStubImpl(name, parentStub, this);
  }

  @Override
  public void serialize(final @NotNull RParameterStub stub, final @NotNull StubOutputStream dataStream)
    throws IOException {
    dataStream.writeName(stub.getName());
  }

  @Override
  public boolean shouldCreateStub(ASTNode node) {
    return !new RParameterImpl(node).getName().equals(RPsiImplUtil.UNNAMED);
  }

  @Override
  public @NotNull RParameterStub deserialize(final @NotNull StubInputStream dataStream, final StubElement parentStub) throws IOException {
    String name = Objects.requireNonNull(StringRef.toString(dataStream.readName()));
    return new RParameterStubImpl(name, parentStub, this);
  }
}
