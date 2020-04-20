// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.roxygen.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface RoxygenNamespaceAccessExpression extends RoxygenExpression {

  @NotNull
  PsiElement getNamespace();

  @NotNull
  RoxygenIdentifierExpression getIdentifier();

  @NotNull
  String getNamespaceName();

}
