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

public class ROperatorExpressionImpl extends RExpressionImpl implements ROperatorExpression {

  public ROperatorExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull RVisitor visitor) {
    visitor.visitOperatorExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RVisitor) accept((RVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public ROperator getOperator() {
    return RPsiImplUtil.getOperator(this);
  }

  @Override
  public boolean isBinary() {
    return RPsiImplUtil.isBinary(this);
  }

  @Override
  @Nullable
  public RExpression getLeftExpr() {
    return RPsiImplUtil.getLeftExpr(this);
  }

  @Override
  @Nullable
  public RExpression getRightExpr() {
    return RPsiImplUtil.getRightExpr(this);
  }

  @Override
  @Nullable
  public RExpression getExpr() {
    return RPsiImplUtil.getExpr(this);
  }

}
