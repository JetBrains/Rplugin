// This is a generated file. Not intended for manual editing.
package com.intellij.r.psi.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface RSubscriptionExpression extends RExpression, RArgumentHolder {

  @NotNull
  List<RExpression> getExpressionList();

  @NotNull
  List<RNamedArgument> getNamedArgumentList();

  boolean isSingle();

}
