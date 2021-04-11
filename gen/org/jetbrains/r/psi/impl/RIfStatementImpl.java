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

public class RIfStatementImpl extends RExpressionImpl implements RIfStatement {

  public RIfStatementImpl(@NotNull ASTNode astNode) {
    super(astNode);
  }

  @Override
  public void accept(@NotNull RVisitor visitor) {
    visitor.visitIfStatement(this);
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
  @Nullable
  public RExpression getCondition() {
    List<RExpression> p1 = getExpressionList();
    return p1.size() < 1 ? null : p1.get(0);
  }

  @Override
  @Nullable
  public RExpression getIfBody() {
    List<RExpression> p1 = getExpressionList();
    return p1.size() < 2 ? null : p1.get(1);
  }

  @Override
  @Nullable
  public RExpression getElseBody() {
    List<RExpression> p1 = getExpressionList();
    return p1.size() < 3 ? null : p1.get(2);
  }

}
