// This is a generated file. Not intended for manual editing.
package com.intellij.r.psi.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface RNamespaceAccessExpression extends RExpression {

  @NotNull
  PsiElement getNamespace();

  @Nullable
  RIdentifierExpression getIdentifier();

  @NotNull
  String getNamespaceName();

}
