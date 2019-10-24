// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.r.psi.references.RReferenceBase;

public interface RIdentifierExpression extends RExpression, PsiNamedElement {

  @NotNull
  RReferenceBase<?> getReference();

  @NotNull
  String getName();

  @NotNull
  PsiElement setName(@NotNull String name);

}
