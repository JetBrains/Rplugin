// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.psi.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.RLanguage;
import org.jetbrains.r.psi.api.RPsiElement;

public abstract class RStubElementType<StubT extends StubElement<?>, PsiT extends RPsiElement> extends IStubElementType<StubT, PsiT> {
  public RStubElementType(@NonNls final String debugName) {
    super(debugName, RLanguage.INSTANCE);
  }

  public abstract PsiElement createElement(@NotNull final ASTNode node);


  @Override
  public void indexStub(@NotNull final StubT stub, @NotNull final IndexSink sink) {
  }

  @Override
  @NotNull
  public String getExternalId() {
    return "r." + super.toString();
  }
}
