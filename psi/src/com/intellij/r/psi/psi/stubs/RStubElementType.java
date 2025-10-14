// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.r.psi.psi.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.r.psi.RLanguage;
import com.intellij.r.psi.psi.api.RPsiElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public abstract class RStubElementType<StubT extends StubElement<?>, PsiT extends RPsiElement> extends IStubElementType<StubT, PsiT> {
  public RStubElementType(final @NonNls String debugName) {
    super(debugName, RLanguage.INSTANCE);
  }

  public abstract PsiElement createElement(final @NotNull ASTNode node);


  @Override
  public void indexStub(final @NotNull StubT stub, final @NotNull IndexSink sink) {
  }

  @Override
  public @NotNull String getExternalId() {
    return "r." + super.toString();
  }
}
