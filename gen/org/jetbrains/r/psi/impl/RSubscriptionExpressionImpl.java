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

public class RSubscriptionExpressionImpl extends RExpressionImpl implements RSubscriptionExpression {

  public RSubscriptionExpressionImpl(@NotNull ASTNode astNode) {
    super(astNode);
  }

  @Override
  public void accept(@NotNull RVisitor visitor) {
    visitor.visitSubscriptionExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RVisitor) accept((RVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<RExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, RExpression.class);
  }

  @Override
  @NotNull
  public List<RNamedArgument> getNamedArgumentList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, RNamedArgument.class);
  }

  @Override
  public boolean isSingle() {
    return RPsiImplUtil.isSingle(this);
  }

}
