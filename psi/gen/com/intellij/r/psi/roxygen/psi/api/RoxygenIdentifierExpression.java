// This is a generated file. Not intended for manual editing.
package com.intellij.r.psi.roxygen.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;

public interface RoxygenIdentifierExpression extends RoxygenExpression, PsiNamedElement {

  @NotNull
  String getName();

  @NotNull
  PsiElement setName(@NotNull String newName);

  @NotNull
  PsiReference getReference();

}
