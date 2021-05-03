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

public class RParameterListImpl extends RElementImpl implements RParameterList {

  public RParameterListImpl(@NotNull ASTNode astNode) {
    super(astNode);
  }

  public void accept(@NotNull RVisitor visitor) {
    visitor.visitParameterList(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RVisitor) accept((RVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<RParameter> getParameterList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, RParameter.class);
  }

}
