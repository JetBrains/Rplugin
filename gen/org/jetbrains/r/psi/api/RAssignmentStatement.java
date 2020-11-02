// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.StubBasedPsiElement;
import org.jetbrains.r.psi.stubs.RAssignmentStub;
import com.intellij.psi.PsiNamedElement;

public interface RAssignmentStatement extends RExpression, PsiNameIdentifierOwner, StubBasedPsiElement<RAssignmentStub> {

  boolean isLeft();

  boolean isRight();

  boolean isEqual();

  @Nullable
  RExpression getAssignedValue();

  @Nullable
  RExpression getAssignee();

  @NotNull
  String getName();

  @NotNull
  PsiElement setName(@NotNull String name);

  @Nullable
  PsiNamedElement getNameIdentifier();

  boolean isFunctionDeclaration();

  boolean isPrimitiveFunctionDeclaration();

  @NotNull
  String getFunctionParameters();

  @NotNull
  List<String> getParameterNameList();

  boolean isClosureAssignment();

}
