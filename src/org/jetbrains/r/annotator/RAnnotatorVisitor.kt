/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.highlighting.*
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.RPsiUtil.isFieldLikeComponent
import org.jetbrains.r.psi.ReferenceKind
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.getKind

class RAnnotatorVisitor(private val holder: AnnotationHolder) : RVisitor() {

  override fun visitCallExpression(callExpression: RCallExpression) {
    val expression = callExpression.expression
    if (expression is RNamespaceAccessExpression && expression.identifier != null) {
      highlight(expression.identifier!!, FUNCTION_CALL)
    }
    else {
      highlight(expression, FUNCTION_CALL)
    }
  }

  override fun visitNamespaceAccessExpression(o: RNamespaceAccessExpression) {
    highlight(o.namespace, NAMESPACE)
  }

  override fun visitIdentifierExpression(element: RIdentifierExpression) {
    analyzeIdentifierExpression(element)?.let { highlight(element, it) }

    if (element.name == "...") {
      if (element.parent !is RCallExpression &&
          PsiTreeUtil.getParentOfType(element, RArgumentList::class.java) == null &&
          !isDotsFunctionDefinition(element) &&
          !declaredDotsParameter(element)) {
        holder.createWarningAnnotation(element, "'...' used in an incorrect context")
      }
    }
  }

  private fun isDotsFunctionDefinition(element: RIdentifierExpression): Boolean =
    (element.parent as? RAssignmentStatement)?.let { it.assignee == element && it.assignedValue is RFunctionExpression } ?: false

  private fun declaredDotsParameter(element: RIdentifierExpression): Boolean {
    var current: RPsiElement = element
    while (true) {
      val rFunctionExpression = PsiTreeUtil.getParentOfType(current, RFunctionExpression::class.java) ?: return false
      for (parameter in rFunctionExpression.parameterList.parameterList) {
        if (parameter.variable?.name == "...") {
          return true
        }
      }
      current = rFunctionExpression
    }
  }

  override fun visitParameter(rParameter: RParameter) {
    highlight(rParameter.nameIdentifier ?: return, PARAMETER)
  }

  override fun visitOperatorExpression(operatorExpression: ROperatorExpression) {
    val right = operatorExpression.rightExpr ?: return
    if (!isFieldLikeComponent(right)) return
    if (right is RStringLiteralExpression || right is RIdentifierExpression) {
      highlight(right, FIELD)
    } else {
      holder.createErrorAnnotation(right, "R grammar doesn't allow `${right.text}` here")
    }
  }

  private fun highlight(element: PsiElement, colorKey: TextAttributesKey) {
    val annotationText = if (ApplicationManager.getApplication().isUnitTestMode) colorKey.externalName else null
    holder.createInfoAnnotation(element, annotationText).textAttributes = colorKey
  }

  private fun analyzeIdentifierExpression(element: RIdentifierExpression): TextAttributesKey? {
    RPsiUtil.getCallByExpression(element)?.let { return null }
    RPsiUtil.getNamedArgumentByNameIdentifier(element)?.let { return NAMED_ARGUMENT }
    RPsiUtil.getAssignmentByAssignee(element)?.let { assignment ->
      return when {
        assignment.isFunctionDeclaration -> FUNCTION_DECLARATION
        assignment.isClosureAssignment -> CLOSURE
        else -> LOCAL_VARIABLE
      }
    }
    return when (element.getKind()) {
      ReferenceKind.LOCAL_VARIABLE -> LOCAL_VARIABLE
      ReferenceKind.CLOSURE -> CLOSURE
      ReferenceKind.PARAMETER -> PARAMETER
      else -> null
    }
  }
}
