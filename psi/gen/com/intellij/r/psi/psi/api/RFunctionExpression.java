// This is a generated file. Not intended for manual editing.
package com.intellij.r.psi.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.r.psi.psi.cfg.RControlFlow;
import com.intellij.r.psi.psi.references.RReferenceBase;

public interface RFunctionExpression extends RExpression, RControlFlowHolder {

  @Nullable
  RExpression getExpression();

  @Nullable
  RParameterList getParameterList();

  @Nullable
  String getDocStringValue();

  @NotNull
  RControlFlow getControlFlow();

  @Nullable
  RReferenceBase<?> getReference();

}
