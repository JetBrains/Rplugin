// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface RForStatement extends RExpression, RLoopStatement {

  @NotNull
  List<RExpression> getExpressionList();

  @Nullable
  RIdentifierExpression getTarget();

  @Nullable
  RExpression getRange();

  @Nullable
  RExpression getBody();

}
