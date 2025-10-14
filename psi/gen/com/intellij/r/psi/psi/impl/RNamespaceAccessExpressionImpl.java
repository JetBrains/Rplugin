// This is a generated file. Not intended for manual editing.
package com.intellij.r.psi.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.r.psi.parsing.RElementTypes.*;
import com.intellij.r.psi.psi.api.*;

public class RNamespaceAccessExpressionImpl extends RExpressionImpl implements RNamespaceAccessExpression {

  public RNamespaceAccessExpressionImpl(@NotNull ASTNode astNode) {
    super(astNode);
  }

  @Override
  public void accept(@NotNull RVisitor visitor) {
    visitor.visitNamespaceAccessExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RVisitor) accept((RVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public PsiElement getNamespace() {
    return notNullChild(findChildByType(R_IDENTIFIER));
  }

  @Override
  @Nullable
  public RIdentifierExpression getIdentifier() {
    return PsiTreeUtil.getChildOfType(this, RIdentifierExpression.class);
  }

  @Override
  @NotNull
  public String getNamespaceName() {
    return RPsiImplUtil.getNamespaceName(this);
  }

}
