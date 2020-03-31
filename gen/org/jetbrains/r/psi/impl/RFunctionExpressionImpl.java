// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static org.jetbrains.r.parsing.RElementTypes.*;
import org.jetbrains.r.psi.api.*;
import org.jetbrains.r.psi.cfg.RControlFlow;
import org.jetbrains.r.psi.references.RReferenceBase;

public class RFunctionExpressionImpl extends RControlFlowHolderImpl implements RFunctionExpression {

  public RFunctionExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull RVisitor visitor) {
    visitor.visitFunctionExpression(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RVisitor) accept((RVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public RExpression getExpression() {
    return PsiTreeUtil.getChildOfType(this, RExpression.class);
  }

  @Override
  @Nullable
  public RParameterList getParameterList() {
    return PsiTreeUtil.getChildOfType(this, RParameterList.class);
  }

  @Override
  @Nullable
  public String getDocStringValue() {
    return RPsiImplUtil.getDocStringValue(this);
  }

  @Override
  @NotNull
  public RControlFlow getControlFlow() {
    return RPsiImplUtil.getControlFlow(this);
  }

  @Override
  @Nullable
  public RReferenceBase<?> getReference() {
    return RPsiImplUtil.getReference(this);
  }

}
