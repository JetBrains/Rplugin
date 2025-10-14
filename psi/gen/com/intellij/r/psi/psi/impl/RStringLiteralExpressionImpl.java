// This is a generated file. Not intended for manual editing.
package com.intellij.r.psi.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.r.psi.parsing.RElementTypes.*;
import com.intellij.r.psi.psi.RStringInjectHost;
import com.intellij.r.psi.psi.api.*;

public class RStringLiteralExpressionImpl extends RStringInjectHost implements RStringLiteralExpression {

  public RStringLiteralExpressionImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull RVisitor visitor) {
    visitor.visitStringLiteralExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RVisitor) accept((RVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public String getName() {
    return RPsiImplUtil.getName(this);
  }

  @Override
  @NotNull
  public PsiElement setName(@NotNull String name) {
    return RPsiImplUtil.setName(this, name);
  }

}
