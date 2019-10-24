// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.r.psi.cfg.RControlFlow;

public interface RFunctionExpression extends RExpression, RControlFlowHolder {

  @Nullable
  RExpression getExpression();

  @NotNull
  RParameterList getParameterList();

  @Nullable
  String getDocStringValue();

  @NotNull
  RControlFlow getControlFlow();

}
