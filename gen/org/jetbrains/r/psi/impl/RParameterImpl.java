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
import org.jetbrains.r.psi.stubs.RParameterStub;
import com.intellij.psi.stubs.IStubElementType;

public class RParameterImpl extends RParameterBase implements RParameter {

  public RParameterImpl(@NotNull ASTNode node) {
    super(node);
  }

  public RParameterImpl(@NotNull RParameterStub stub, @NotNull IStubElementType<?, ?> nodeType) {
    super(stub, nodeType);
  }

  public void accept(@NotNull RVisitor visitor) {
    visitor.visitParameter(this);
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
  public RIdentifierExpression getNameIdentifier() {
    return RPsiImplUtil.getNameIdentifier(this);
  }

  @Override
  @Nullable
  public RIdentifierExpression getVariable() {
    return RPsiImplUtil.getVariable(this);
  }

  @Override
  @Nullable
  public RExpression getDefaultValue() {
    return RPsiImplUtil.getDefaultValue(this);
  }

}
