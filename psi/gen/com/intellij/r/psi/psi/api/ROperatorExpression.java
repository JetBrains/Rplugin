// This is a generated file. Not intended for manual editing.
package com.intellij.r.psi.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface ROperatorExpression extends RExpression {

  @Nullable
  ROperator getOperator();

  boolean isBinary();

  @Nullable
  RExpression getLeftExpr();

  @Nullable
  RExpression getRightExpr();

  @Nullable
  RExpression getExpr();

}
