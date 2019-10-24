// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.r.psi.references.ROperatorReference;

public interface ROperator extends RPsiElement {

  @NotNull
  String getName();

  @NotNull
  ROperatorReference getReference();

}
