// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface RWhileStatement extends RExpression, RLoopStatement {

  @NotNull
  List<RExpression> getExpressionList();

  @Nullable
  RExpression getCondition();

  @Nullable
  RExpression getBody();

}
