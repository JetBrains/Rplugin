/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.r.RBundle
import org.jetbrains.r.highlighting.*
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.RPsiUtil.isFieldLikeComponent
import org.jetbrains.r.psi.ReferenceKind
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.getKind
import org.jetbrains.r.psi.isFunctionFromLibrary

class RAnnotatorVisitor(private val holder: AnnotationHolder) : RVisitor() {

  override fun visitCallExpression(callExpression: RCallExpression) {
    val expression = callExpression.expression
    if (expression is RNamespaceAccessExpression && expression.identifier != null) {
      highlight(expression.identifier!!, FUNCTION_CALL)
    } else if (expression is RIdentifierExpression) {
      highlight(expression, FUNCTION_CALL)
    }
    // otherwise it's a complex expression, lets use the default highlighting for it
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
        holder.createWarningAnnotation(element, RBundle.message("inspection.message.used.in.incorrect.context"))
      }
    }
  }

  private fun isDotsFunctionDefinition(element: RIdentifierExpression): Boolean =
    (element.parent as? RAssignmentStatement)?.let { it.assignee == element && it.assignedValue is RFunctionExpression } ?: false

  private fun declaredDotsParameter(element: RIdentifierExpression): Boolean {
    var current: RPsiElement = element
    while (true) {
      val rFunctionExpression = PsiTreeUtil.getParentOfType(current, RFunctionExpression::class.java) ?: return false
      val parameterList = rFunctionExpression.parameterList ?: return false
      for (parameter in parameterList.parameterList) {
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
      holder.createErrorAnnotation(right, RBundle.message("inspection.message.r.grammar.doesn.t.allow.here", right.text))
    }
  }

  override fun visitNoCommaTail(noComma: RNoCommaTail) {
    val reportElement = PsiTreeUtil.findChildOfAnyType(noComma, RExpression::class.java, RNamedArgument::class.java) ?: return
    val offset = reportElement.textRange.startOffset
    holder.createErrorAnnotation(TextRange(offset, offset + 1), RBundle.message("inspection.message.missing.comma"))
  }

  override fun visitStringLiteralExpression(o: RStringLiteralExpression) {
    val call = PsiTreeUtil.skipParentsOfType(o, RNamedArgument::class.java, RArgumentList::class.java)
    if (call !is RCallExpression) return

    // To avoid IndexNotReadyException
    if (DumbService.isDumb(o.project)) return
    if (!call.isFunctionFromLibrary("source", "base")) return
    if (RParameterInfoUtil.getArgumentByName(call, "file") != o) return

    val reference = o.references.filterIsInstance<FileReference>().singleOrNull() ?: return
    val range = reference.rangeInElement
    if (range.isEmpty) return

    var message = holder.currentAnnotationSession.getUserData(sourceTooltipMessageKey)
    if (message == null) {
      message = sourceTooltipMessage
      holder.currentAnnotationSession.putUserData(sourceTooltipMessageKey, message)
    }
    val annotation = holder.createInfoAnnotation(range.shiftRight(o.textOffset), message)
    annotation.textAttributes = CodeInsightColors.INACTIVE_HYPERLINK_ATTRIBUTES
  }

  override fun visitInvalidLiteral(invalid: RInvalidLiteral) {
    holder.createErrorAnnotation(invalid, RBundle.message("inspection.message.unclosed.string.literal"))
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
    return element.textAttribute
  }

  companion object {
    private val sourceTooltipMessageKey = Key.create<String>("r.source.file.message")

    private val sourceTooltipMessage: String
      get() {
        val messagePrefix = RBundle.message("open.source.file.in.editor.tooltip")
        val shortcuts = KeymapManager.getInstance().activeKeymap.getShortcuts(IdeActions.ACTION_GOTO_DECLARATION)
        val shortcutText = buildString {
          ContainerUtil.find(shortcuts) { !it.isKeyboard }?.let {
            append(KeymapUtil.getShortcutText(it).replace(Regex("Button\\d "), ""))
          }

          ContainerUtil.find(shortcuts) { it.isKeyboard }?.let {
            if (isNotEmpty()) append(", ")
            append(KeymapUtil.getShortcutText(it))
          }
        }

        return if (shortcutText.isNotEmpty()) messagePrefix + " ($shortcutText)"
        else messagePrefix
      }
  }
}

internal val RIdentifierExpression.textAttribute: TextAttributesKey?
  get() = when (getKind()) {
    ReferenceKind.LOCAL_VARIABLE -> LOCAL_VARIABLE
    ReferenceKind.CLOSURE -> CLOSURE
    ReferenceKind.PARAMETER -> PARAMETER
    else -> null
  }