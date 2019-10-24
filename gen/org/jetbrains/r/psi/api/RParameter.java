// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.StubBasedPsiElement;
import org.jetbrains.r.psi.stubs.RParameterStub;

public interface RParameter extends PsiNameIdentifierOwner, RPsiElement, StubBasedPsiElement<RParameterStub> {

  @NotNull
  List<RExpression> getExpressionList();

  @NotNull
  String getName();

  @NotNull
  PsiElement setName(@NotNull String name);

  @Nullable
  RIdentifierExpression getNameIdentifier();

  @Nullable
  RIdentifierExpression getVariable();

  @Nullable
  RExpression getDefaultValue();

}
