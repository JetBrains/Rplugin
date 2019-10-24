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
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.r.psi.stubs.RAssignmentStub;
import com.intellij.psi.stubs.IStubElementType;

public class RAssignmentStatementImpl extends RAssignmentBase implements RAssignmentStatement {

  public RAssignmentStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public RAssignmentStatementImpl(@NotNull RAssignmentStub stub, @NotNull IStubElementType type) {
    super(stub, type);
  }

  public void accept(@NotNull RVisitor visitor) {
    visitor.visitAssignmentStatement(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RVisitor) accept((RVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  public boolean isLeft() {
    return RPsiImplUtil.isLeft(this);
  }

  @Override
  public boolean isRight() {
    return RPsiImplUtil.isRight(this);
  }

  @Override
  public boolean isEqual() {
    return RPsiImplUtil.isEqual(this);
  }

  @Override
  @Nullable
  public RExpression getAssignedValue() {
    return RPsiImplUtil.getAssignedValue(this);
  }

  @Override
  @Nullable
  public RExpression getAssignee() {
    return RPsiImplUtil.getAssignee(this);
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
  public boolean isFunctionDeclaration() {
    return RPsiImplUtil.isFunctionDeclaration(this);
  }

  @Override
  @NotNull
  public String getFunctionParameters() {
    return RPsiImplUtil.getFunctionParameters(this);
  }

  @Override
  @NotNull
  public List<String> getParameterNameList() {
    return RPsiImplUtil.getParameterNameList(this);
  }

  @Override
  public boolean isClosureAssignment() {
    return RPsiImplUtil.isClosureAssignment(this);
  }

}
