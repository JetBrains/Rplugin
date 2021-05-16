// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.psi.api;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiNamedElement;

public class RVisitor extends PsiElementVisitor {

  public void visitAndOperator(@NotNull RAndOperator o) {
    visitOperator(o);
  }

  public void visitArgumentList(@NotNull RArgumentList o) {
    visitArgumentHolder(o);
  }

  public void visitAssignOperator(@NotNull RAssignOperator o) {
    visitOperator(o);
  }

  public void visitAssignmentStatement(@NotNull RAssignmentStatement o) {
    visitExpression(o);
    // visitPsiNameIdentifierOwner(o);
    // visitS4GenericOrMethodHolder(o);
  }

  public void visitAtExpression(@NotNull RAtExpression o) {
    visitOperatorExpression(o);
  }

  public void visitAtOperator(@NotNull RAtOperator o) {
    visitOperator(o);
  }

  public void visitBlockExpression(@NotNull RBlockExpression o) {
    visitExpression(o);
  }

  public void visitBooleanLiteral(@NotNull RBooleanLiteral o) {
    visitExpression(o);
  }

  public void visitBoundaryLiteral(@NotNull RBoundaryLiteral o) {
    visitExpression(o);
  }

  public void visitBreakStatement(@NotNull RBreakStatement o) {
    visitExpression(o);
  }

  public void visitCallExpression(@NotNull RCallExpression o) {
    visitExpression(o);
    // visitS4GenericOrMethodHolder(o);
  }

  public void visitColonOperator(@NotNull RColonOperator o) {
    visitOperator(o);
  }

  public void visitCompareOperator(@NotNull RCompareOperator o) {
    visitOperator(o);
  }

  public void visitEmptyExpression(@NotNull REmptyExpression o) {
    visitExpression(o);
  }

  public void visitExpOperator(@NotNull RExpOperator o) {
    visitOperator(o);
  }

  public void visitExpression(@NotNull RExpression o) {
    visitPsiElement(o);
  }

  public void visitForStatement(@NotNull RForStatement o) {
    visitExpression(o);
    // visitLoopStatement(o);
  }

  public void visitFunctionExpression(@NotNull RFunctionExpression o) {
    visitExpression(o);
    // visitControlFlowHolder(o);
    // visitExpression(o);
  }

  public void visitHelpExpression(@NotNull RHelpExpression o) {
    visitExpression(o);
  }

  public void visitIdentifierExpression(@NotNull RIdentifierExpression o) {
    visitExpression(o);
    // visitPsiNamedElement(o);
  }

  public void visitIfStatement(@NotNull RIfStatement o) {
    visitExpression(o);
  }

  public void visitInfixOperator(@NotNull RInfixOperator o) {
    visitOperator(o);
    // visitPsiNamedElement(o);
  }

  public void visitInvalidLiteral(@NotNull RInvalidLiteral o) {
    visitExpression(o);
  }

  public void visitListSubsetOperator(@NotNull RListSubsetOperator o) {
    visitOperator(o);
  }

  public void visitMemberExpression(@NotNull RMemberExpression o) {
    visitOperatorExpression(o);
  }

  public void visitMuldivOperator(@NotNull RMuldivOperator o) {
    visitOperator(o);
  }

  public void visitNaLiteral(@NotNull RNaLiteral o) {
    visitExpression(o);
  }

  public void visitNamedArgument(@NotNull RNamedArgument o) {
    visitPsiNameIdentifierOwner(o);
    // visitExpression(o);
  }

  public void visitNamespaceAccessExpression(@NotNull RNamespaceAccessExpression o) {
    visitExpression(o);
  }

  public void visitNextStatement(@NotNull RNextStatement o) {
    visitExpression(o);
  }

  public void visitNoCommaTail(@NotNull RNoCommaTail o) {
    visitPsiElement(o);
  }

  public void visitNotOperator(@NotNull RNotOperator o) {
    visitOperator(o);
  }

  public void visitNullLiteral(@NotNull RNullLiteral o) {
    visitExpression(o);
  }

  public void visitNumericLiteralExpression(@NotNull RNumericLiteralExpression o) {
    visitExpression(o);
  }

  public void visitOperator(@NotNull ROperator o) {
    visitPsiElement(o);
  }

  public void visitOperatorExpression(@NotNull ROperatorExpression o) {
    visitExpression(o);
  }

  public void visitOrOperator(@NotNull ROrOperator o) {
    visitOperator(o);
  }

  public void visitParameter(@NotNull RParameter o) {
    visitPsiNameIdentifierOwner(o);
  }

  public void visitParameterList(@NotNull RParameterList o) {
    visitPsiElement(o);
  }

  public void visitParenthesizedExpression(@NotNull RParenthesizedExpression o) {
    visitExpression(o);
  }

  public void visitPlusminusOperator(@NotNull RPlusminusOperator o) {
    visitOperator(o);
  }

  public void visitRepeatStatement(@NotNull RRepeatStatement o) {
    visitExpression(o);
    // visitLoopStatement(o);
  }

  public void visitStringLiteralExpression(@NotNull RStringLiteralExpression o) {
    visitExpression(o);
    // visitPsiNamedElement(o);
  }

  public void visitSubscriptionExpression(@NotNull RSubscriptionExpression o) {
    visitExpression(o);
    // visitArgumentHolder(o);
  }

  public void visitTildeExpression(@NotNull RTildeExpression o) {
    visitExpression(o);
  }

  public void visitTildeOperator(@NotNull RTildeOperator o) {
    visitOperator(o);
  }

  public void visitUnaryNotExpression(@NotNull RUnaryNotExpression o) {
    visitOperatorExpression(o);
  }

  public void visitUnaryPlusminusExpression(@NotNull RUnaryPlusminusExpression o) {
    visitOperatorExpression(o);
  }

  public void visitUnaryTildeExpression(@NotNull RUnaryTildeExpression o) {
    visitOperatorExpression(o);
  }

  public void visitWhileStatement(@NotNull RWhileStatement o) {
    visitExpression(o);
    // visitLoopStatement(o);
  }

  public void visitPsiNameIdentifierOwner(@NotNull PsiNameIdentifierOwner o) {
    visitElement(o);
  }

  public void visitArgumentHolder(@NotNull RArgumentHolder o) {
    visitPsiElement(o);
  }

  public void visitPsiElement(@NotNull RPsiElement o) {
    visitElement(o);
  }

}
