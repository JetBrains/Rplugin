// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.psi.api;

import org.jetbrains.annotations.*;
import com.intellij.psi.StubBasedPsiElement;
import org.jetbrains.r.psi.stubs.RCallExpressionStub;
import org.jetbrains.r.classes.s4.RS4ClassInfo;

public interface RCallExpression extends RExpression, StubBasedPsiElement<RCallExpressionStub> {

  @NotNull
  RArgumentList getArgumentList();

  @NotNull
  RExpression getExpression();

  @Nullable
  RS4ClassInfo getAssociatedS4ClassInfo();

}
