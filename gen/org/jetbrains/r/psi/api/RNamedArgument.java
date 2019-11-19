// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.r.psi.references.RReferenceBase;

public interface RNamedArgument extends PsiNameIdentifierOwner, RExpression {

  @NotNull
  RAssignOperator getAssignOperator();

  @NotNull
  List<RExpression> getExpressionList();

  @NotNull
  String getName();

  @NotNull
  PsiElement setName(@NotNull String name);

  @Nullable
  PsiNamedElement getNameIdentifier();

  @Nullable
  RExpression getAssignedValue();

  @Nullable
  RReferenceBase<?> getReference();

}
