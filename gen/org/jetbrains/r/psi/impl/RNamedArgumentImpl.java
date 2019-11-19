// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static org.jetbrains.r.parsing.RElementTypes.*;
import org.jetbrains.r.psi.RElementImpl;
import org.jetbrains.r.psi.api.*;
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.r.psi.references.RReferenceBase;

public class RNamedArgumentImpl extends RElementImpl implements RNamedArgument {

  public RNamedArgumentImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull RVisitor visitor) {
    visitor.visitNamedArgument(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RVisitor) accept((RVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public RAssignOperator getAssignOperator() {
    return notNullChild(PsiTreeUtil.getChildOfType(this, RAssignOperator.class));
  }

  @Override
  @NotNull
  public List<RExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, RExpression.class);
  }

  @Override
  @NotNull
  public String getName() {
    return RPsiImplUtil.getName(this);
  }

  @Override
  @NotNull
  public PsiElement setName(@NotNull String name) {
    return RPsiImplUtil.setName(this, name);
  }

  @Override
  @Nullable
  public PsiNamedElement getNameIdentifier() {
    return RPsiImplUtil.getNameIdentifier(this);
  }

  @Override
  @Nullable
  public RExpression getAssignedValue() {
    return RPsiImplUtil.getAssignedValue(this);
  }

  @Override
  @Nullable
  public RReferenceBase<?> getReference() {
    return RPsiImplUtil.getReference(this);
  }

}
