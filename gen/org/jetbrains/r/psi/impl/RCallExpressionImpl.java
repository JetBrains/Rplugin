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
import org.jetbrains.r.psi.stubs.RCallExpressionStub;
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
