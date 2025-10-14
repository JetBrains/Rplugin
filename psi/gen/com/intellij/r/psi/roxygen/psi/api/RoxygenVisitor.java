// This is a generated file. Not intended for manual editing.
package com.intellij.r.psi.roxygen.psi.api;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiNamedElement;

public class RoxygenVisitor extends PsiElementVisitor {

  public void visitAutolink(@NotNull RoxygenAutolink o) {
    visitPsiElement(o);
  }

  public void visitExpression(@NotNull RoxygenExpression o) {
    visitPsiElement(o);
  }

  public void visitHelpPageLink(@NotNull RoxygenHelpPageLink o) {
    visitPsiElement(o);
  }

  public void visitIdentifierExpression(@NotNull RoxygenIdentifierExpression o) {
    visitExpression(o);
    // visitPsiNamedElement(o);
  }

  public void visitLinkDestination(@NotNull RoxygenLinkDestination o) {
    visitPsiElement(o);
  }

  public void visitNamespaceAccessExpression(@NotNull RoxygenNamespaceAccessExpression o) {
    visitExpression(o);
  }

  public void visitParamTag(@NotNull RoxygenParamTag o) {
    visitTag(o);
  }

  public void visitParameter(@NotNull RoxygenParameter o) {
    visitExpression(o);
    // visitPsiNamedElement(o);
  }

  public void visitTag(@NotNull RoxygenTag o) {
    visitPsiElement(o);
  }

  public void visitPsiElement(@NotNull RoxygenPsiElement o) {
    visitElement(o);
  }

}
