/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.classes.s4.methods

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.r.psi.classes.s4.RS4Resolver
import com.intellij.r.psi.classes.s4.RS4Util
import com.intellij.r.psi.classes.s4.classInfo.associatedS4ClassInfo
import com.intellij.r.psi.hints.parameterInfo.RArgumentInfo
import com.intellij.r.psi.psi.api.*
import com.intellij.r.psi.psi.impl.RCallExpressionImpl
import com.intellij.r.psi.psi.isFunctionFromLibrarySoft
import com.intellij.r.psi.skeleton.psi.RSkeletonAssignmentStatement

object RS4MethodsUtil {

  fun parseS4GenericOrMethodInfo(callExpression: RCallExpression): RS4GenericOrMethodInfo? {
    val project = callExpression.project
    return when {
      callExpression.isFunctionFromLibrarySoft("setGeneric", "methods") -> {
        val argumentsInfo = RArgumentInfo.getArgumentInfo(callExpression, project.setGenericDefinition) ?: return null
        val methodName = (argumentsInfo.getArgumentPassedToParameter("name") as? RStringLiteralExpression)?.name ?: return null
        val (params, partialParsedParams) = parseCharacterVector(argumentsInfo.getArgumentPassedToParameter("signature"))
        val (valueClasses, partialParsedValueClasses) = parseCharacterVector(argumentsInfo.getArgumentPassedToParameter("valueClass"))
        RS4GenericInfo(
          methodName,
          RS4GenericSignature(
            params ?: argumentsInfo.findFunctionParameters("def"),
            valueClasses ?: emptyList(),
            partialParsedParams || partialParsedValueClasses
          )
        )
      }
      callExpression.isFunctionFromLibrarySoft("setMethod", "methods") -> {
        val argumentsInfo = RArgumentInfo.getArgumentInfo(callExpression, project.setMethodDefinition) ?: return null
        val methodName = argumentsInfo.getArgumentPassedToParameter("f")?.let {
          when (it) {
            is RStringLiteralExpression -> it.name
            is RIdentifierExpression -> it.name // works cause of resolve to generic
            else -> null
          }
        } ?: return null

        val signature = RS4Util.parseCharacterVector(argumentsInfo.getArgumentPassedToParameter("signature"))
        RS4SignatureMethodInfo(methodName, signature?.map { it.text })
      }
      else -> null
    }
  }

  private val RS4GenericOrMethodHolder.associatedS4GenericOrMethodInfo: RS4GenericOrMethodInfo?
    get() = when (this) {
      is RCallExpressionImpl -> {
        val stub = greenStub
        if (stub != null) stub.s4GenericOrMethodInfo
        else parseS4GenericOrMethodInfo(this)
      }
      is RSkeletonAssignmentStatement -> stub.s4GenericOrMethodInfo
      else -> null
    }

  val RS4GenericOrMethodHolder.associatedS4GenericInfo: RS4GenericInfo?
    get() = associatedS4GenericOrMethodInfo as? RS4GenericInfo

  val RS4GenericOrMethodHolder.associatedS4MethodInfo: RS4MethodInfo?
    get() = associatedS4GenericOrMethodInfo as? RS4MethodInfo

  val RS4GenericOrMethodHolder.methodNameIdentifier: RExpression?
    get() = when (this) {
      is RCallExpressionImpl -> RArgumentInfo.getArgumentInfo(this)?.getArgumentPassedToParameter(0)
      else -> null
    }

  val RS4GenericOrMethodHolder.methodName: String?
    get() = when (this) {
      is RCallExpressionImpl -> (methodNameIdentifier as? RStringLiteralExpression)?.name
      is RSkeletonAssignmentStatement -> stub.name
      else -> null
    }

  fun RArgumentInfo.toS4MethodParameters(isDeclaration: Boolean): List<RS4MethodParameterInfo> {
    return (argumentNamesWithPipeExpression zip expressionList).mapNotNull { (name, expr) ->
      name ?: return@mapNotNull null
      val className =
        if (isDeclaration) {
          when (expr) {
            is RStringLiteralExpression -> expr.name
            is RNamedArgument -> (expr.assignedValue as? RStringLiteralExpression)?.name
            else -> null
          }
        }
        else {
          val typeClass = RS4Resolver.findElementS4ClassDeclarations(expr).singleOrNull()
          typeClass?.associatedS4ClassInfo?.className
        } ?: return@mapNotNull null
      RS4MethodParameterInfo(name, className)
    }
  }

  private fun RArgumentInfo.findFunctionParameters(parameterName: String): List<String> {
    return when (val defArg = getArgumentPassedToParameter(parameterName)) {
             is RFunctionExpression -> defArg.parameterList?.parameterList?.map { it.name }
             is RIdentifierExpression -> (defArg.reference.resolve() as? RAssignmentStatement)?.parameterNameList
             else -> null
           } ?: emptyList()
  }

  private fun parseCharacterVector(expr: RExpression?): Pair<List<String>?, Boolean> {
    var partialParsed = false
    val params = RS4Util.parseCharacterVector(expr)?.mapNotNull { it ->
      when (it) {
        is RNamedArgument -> (it.assignedValue as? RStringLiteralExpression)?.name
        is RStringLiteralExpression -> it.name
        else -> null
      }.also { name -> if (name == null) partialParsed = true }
    }
    return params to partialParsed
  }

  private val Project.setGenericDefinition: RAssignmentStatement
    get() = RS4Util.run { getProjectCachedAssignment(SET_GENERIC_DEFINITION_KEY, SET_GENERIC_DEFINITION) }

  private val Project.setMethodDefinition: RAssignmentStatement
    get() = RS4Util.run { getProjectCachedAssignment(SET_METHOD_DEFINITION_KEY, SET_METHOD_DEFINITION) }

  private val SET_GENERIC_DEFINITION =
    """setGeneric <- function(name, def = NULL, group = list(), valueClass = character(), 
                              where = topenv(parent.frame()), package = NULL, signature = NULL, 
                              useAsDefault = NULL, genericFunction = NULL, simpleInheritanceOnly = NULL) {}""".trimIndent()

  private val SET_GENERIC_DEFINITION_KEY: Key<RAssignmentStatement> = Key.create("S4_SET_GENERIC_DEFINITION")

  private val SET_METHOD_DEFINITION =
    """setMethod <- function (f, signature = character(), definition, 
                              where = topenv(parent.frame()), valueClass = NULL, 
                              sealed = FALSE) {}""".trimIndent()

  private val SET_METHOD_DEFINITION_KEY: Key<RAssignmentStatement> = Key.create("S4_SET_METHOD_DEFINITION")
}