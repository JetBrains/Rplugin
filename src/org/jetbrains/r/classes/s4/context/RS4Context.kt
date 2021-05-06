package org.jetbrains.r.classes.s4.context

import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RPsiElement

interface RS4Context {
  val contextFunctionName: String
  val contextFunctionCall: RCallExpression
  val originalElement: RPsiElement
}