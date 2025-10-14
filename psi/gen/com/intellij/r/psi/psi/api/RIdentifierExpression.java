// This is a generated file. Not intended for manual editing.
package com.intellij.r.psi.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.r.psi.psi.references.RReferenceBase;

public interface RIdentifierExpression extends RExpression, PsiNamedElement {

  @NotNull
  RReferenceBase<?> getReference();

  @NotNull
  String getName();

  @NotNull
  PsiElement setName(@NotNull String name);

}
