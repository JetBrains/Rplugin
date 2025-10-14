// This is a generated file. Not intended for manual editing.
package com.intellij.r.psi.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.r.psi.parsing.RElementTypes.*;
import com.intellij.r.psi.psi.RElementImpl;
import com.intellij.r.psi.psi.api.*;
import com.intellij.r.psi.psi.references.ROperatorReference;

public class ROperatorImpl extends RElementImpl implements ROperator {

  public ROperatorImpl(@NotNull ASTNode astNode) {
    super(astNode);
  }

  public void accept(@NotNull RVisitor visitor) {
    visitor.visitOperator(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RVisitor) accept((RVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public String getName() {
    return RPsiImplUtil.getName(this);
  }

  @Override
  @NotNull
  public ROperatorReference getReference() {
    return RPsiImplUtil.getReference(this);
  }

}
