// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
import org.jetbrains.r.psi.stubs.RCallExpressionStub;

public interface RCallExpression extends RExpression, RS4GenericOrMethodHolder, StubBasedPsiElement<RCallExpressionStub> {

  @NotNull
  RArgumentList getArgumentList();

  @NotNull
  RExpression getExpression();

}
