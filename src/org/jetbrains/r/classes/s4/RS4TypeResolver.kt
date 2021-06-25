/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.s4

import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.jetbrains.r.classes.s4.classInfo.RS4ClassInfoUtil
import org.jetbrains.r.classes.s4.classInfo.RS4ComplexSlotPomTarget
import org.jetbrains.r.classes.s4.classInfo.RSkeletonS4SlotPomTarget
import org.jetbrains.r.classes.s4.classInfo.RStringLiteralPomTarget
import org.jetbrains.r.classes.s4.context.RS4ContextProvider
import org.jetbrains.r.classes.s4.context.RS4NewObjectSlotNameContext
import org.jetbrains.r.classes.s4.context.methods.RS4SetMethodDefinitionContext
import org.jetbrains.r.classes.s4.methods.RS4MethodsUtil.associatedS4MethodInfo
import org.jetbrains.r.parsing.RElementTypes
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.isFunctionFromLibrarySoft
import org.jetbrains.r.psi.references.RSearchScopeUtil

object RS4TypeResolver {

  fun resolveS4TypeClass(element: RPsiElement): List<String> {
    return when (element) {
      is RNumericLiteralExpression, is RBoundaryLiteral -> NUMERIC
      is RStringLiteralExpression -> CHARACTER
      is RNullLiteral -> NULL
      is RNaLiteral -> when (element.elementType) {
        RElementTypes.R_NA -> LOGICAL
        RElementTypes.R_NA_CHARACTER_ -> CHARACTER
        RElementTypes.R_NA_COMPLEX_ -> COMPLEX
        RElementTypes.R_NA_INTEGER_ -> INTEGER
        RElementTypes.R_NA_REAL_ -> NUMERIC
        else -> emptyList()
      }
      is RFunctionExpression -> FUNCTION
      is RAssignmentStatement -> element.assignedValue?.let { resolveS4TypeClass(it) }
      is RParameter -> {
        val fn = PsiTreeUtil.getParentOfType(element, RFunctionExpression::class.java) ?: return emptyList()
        val context = RS4ContextProvider.getS4Context(fn, RS4SetMethodDefinitionContext::class) ?: return emptyList()
        val params = context.functionCall.associatedS4MethodInfo?.getParameters(RSearchScopeUtil.getScope(element))
        params?.firstOrNull { it.name == element.name }?.type?.let { listOf(it) }
      }
      is RIdentifierExpression -> {
        val newSlotContext = RS4ContextProvider.getS4Context(element, RS4NewObjectSlotNameContext::class)
        if (newSlotContext != null) {
          RS4ClassInfoUtil.getAssociatedClassName(newSlotContext.functionCall)?.let { listOf(it) }
        }
        else element.reference.multiResolve(false).flatMap {
          (it.element as? RPsiElement)?.let { it1 -> resolveS4TypeClass(it1) } ?: emptyList()
        }
      }
      is RAtExpression -> {
        val ownerIdentifier = element.rightExpr as? RIdentifierExpression ?: return emptyList()
        ownerIdentifier.reference.multiResolve(false).mapNotNull { resolveResult ->
          when (val resolveElement = resolveResult.element) {
            is RNamedArgument -> resolveElement.assignedValue?.name
            is PomTargetPsiElement -> {
              when (val target = resolveElement.target) {
                is RSkeletonS4SlotPomTarget -> target.setClass.stub.s4ClassInfo.slots.firstOrNull { it.name == target.name }?.type
                is RS4ComplexSlotPomTarget -> target.slot.type
                is RStringLiteralPomTarget -> "ANY"
                else -> null
              }
            }
            else -> null
          }
        }
      }
      is RCallExpression -> when {
        element.isFunctionFromLibrarySoft("new", "methods") -> {
          val className = RS4ClassInfoUtil.getAssociatedClassName(element) ?: return emptyList()
          listOf(className)
        }
        element.isFunctionFromLibrarySoft("list", "base") -> LIST
        element.isFunctionFromLibrarySoft("c", "base") ->
          element.argumentList.expressionList.asSequence().map { resolveS4TypeClass(it) }.filter { it.isNotEmpty() }.firstOrNull()
        element.isFunctionFromLibrarySoft("data.frame", "base") -> DATA_FRAME
        element.isFunctionFromLibrarySoft("tibble", "tibble") -> TBL
        element.isFunctionFromLibrarySoft("data.table", "data.table") -> DATA_TABLE
        else -> null
      }
      is ROperatorExpression -> when(element.operator) {
        is RCompareOperator, is ROrOperator, is RAndOperator, is RNotOperator -> LOGICAL
        is RPlusminusOperator, is RMuldivOperator, is RExpOperator -> {
          val leftType by lazy { element.leftExpr?.let { resolveS4TypeClass(it) } ?: NUMERIC }
          val rightType by lazy { element.rightExpr?.let { resolveS4TypeClass(it) } ?: NUMERIC }
          val unaryType by lazy { element.expr?.let { resolveS4TypeClass(it) } ?: NUMERIC }
          if (leftType == NUMERIC && rightType == NUMERIC && unaryType == NUMERIC) NUMERIC
          else null
        }
        else -> null
      }
      else -> null
    } ?: return emptyList()
  }

  private val NUMERIC = listOf("numeric")
  private val NULL = listOf("NULL")
  private val FUNCTION = listOf("function")
  private val CHARACTER = listOf("character")
  private val COMPLEX = listOf("complex")
  private val INTEGER = listOf("integer")
  private val LOGICAL = listOf("logical")
  private val LIST = listOf("list")
  private val DATA_FRAME = listOf("data.frame")
  private val TBL = listOf("tbl_df", "tbl", "data.frame")
  private val DATA_TABLE = listOf("data.table", "data.frame")
}