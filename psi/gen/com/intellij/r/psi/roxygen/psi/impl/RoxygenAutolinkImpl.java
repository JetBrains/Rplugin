// This is a generated file. Not intended for manual editing.
package com.intellij.r.psi.roxygen.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.*;
import com.intellij.r.psi.roxygen.psi.RoxygenElementImpl;
import com.intellij.r.psi.roxygen.psi.api.*;

public class RoxygenAutolinkImpl extends RoxygenElementImpl implements RoxygenAutolink {

  public RoxygenAutolinkImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull RoxygenVisitor visitor) {
    visitor.visitAutolink(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RoxygenVisitor) accept((RoxygenVisitor)visitor);
    else super.accept(visitor);
  }

}
