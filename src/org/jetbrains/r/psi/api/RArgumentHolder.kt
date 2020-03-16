package org.jetbrains.r.psi.api

interface RArgumentHolder : RPsiElement {
  val expressionList: MutableList<RExpression>
  val namedArgumentList: MutableList<RNamedArgument>
}