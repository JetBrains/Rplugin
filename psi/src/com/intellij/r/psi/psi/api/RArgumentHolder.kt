package com.intellij.r.psi.psi.api

interface RArgumentHolder : RPsiElement {
  val expressionList: MutableList<RExpression>
  val namedArgumentList: MutableList<RNamedArgument>
}