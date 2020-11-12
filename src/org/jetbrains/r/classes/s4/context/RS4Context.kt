package org.jetbrains.r.classes.s4.context

import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RPsiElement

interface RS4Context {
  val functionName: String
  val functionCall: RCallExpression
  val argumentInfo: RArgumentInfo
  val originalElement: RPsiElement
}