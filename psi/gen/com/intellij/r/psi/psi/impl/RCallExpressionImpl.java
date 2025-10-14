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
import com.intellij.r.psi.psi.stubs.RCallExpressionStub;
import com.intellij.psi.stubs.IStubElementType;

public class RCallExpressionImpl extends RCallExpressionBase implements RCallExpression {

  public RCallExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public RCallExpressionImpl(@NotNull RCallExpressionStub stub, @NotNull IStubElementType<?, ?> nodeType) {
    super(stub, nodeType);
  }

  public void accept(@NotNull RVisitor visitor) {
    visitor.visitCallExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RVisitor) accept((RVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public RArgumentList getArgumentList() {
    return RPsiImplUtil.getArgumentList(this);
  }

  @Override
  @NotNull
  public RExpression getExpression() {
    return RPsiImplUtil.getExpression(this);
  }

}
