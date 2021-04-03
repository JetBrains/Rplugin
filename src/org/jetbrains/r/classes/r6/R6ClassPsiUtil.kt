/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.r6

import com.intellij.psi.util.elementType
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil
import org.jetbrains.r.parsing.RElementTypes.R_MEMBER_EXPRESSION
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.impl.RCallExpressionImpl

object R6ClassPsiUtil {

  /**
   * @param dependantIdentifier `someMember` psi-element of expression `obj$someMember`
   * @return RPsiElement with name of `someMember`
   */
  fun getSearchedIdentifier(dependantIdentifier: RIdentifierExpression?) : RPsiElement? {
    if (dependantIdentifier == null) return null

    val objectDeclarationStatement = getClassInstantiationExpression(dependantIdentifier) ?: return null
    val classDefinition = getClassDefinitionExpression(objectDeclarationStatement) ?: return null
    val argumentInfo = getClassDefinitionArgumentInfo(classDefinition) ?: return null

    val publicMembersCall = argumentInfo.getArgumentPassedToParameter(R6ClassInfoUtil.argumentPublic) as? RCallExpressionImpl
    val privateMembersCall = argumentInfo.getArgumentPassedToParameter(R6ClassInfoUtil.argumentPrivate) as? RCallExpressionImpl
    val activeMembersCall = argumentInfo.getArgumentPassedToParameter(R6ClassInfoUtil.argumentActive) as? RCallExpressionImpl

    val publicMembers = publicMembersCall?.argumentList?.expressionList
    val privateMembers = privateMembersCall?.argumentList?.expressionList
    val activeMembers = activeMembersCall?.argumentList?.expressionList

    return extractNamedArgumentByName(dependantIdentifier.name, publicMembers?.map { it as RNamedArgument })
           ?: extractNamedArgumentByName(dependantIdentifier.name, privateMembers?.map { it as RNamedArgument })
           ?: extractNamedArgumentByName(dependantIdentifier.name, activeMembers?.map { it as RNamedArgument })
  }

  /**
   * @param dependantIdentifier `someMember` psi-element of expression `obj$someMember`
   * @return RAssignmentStatement `obj <- MyClass$new()`
   */
  private fun getClassInstantiationExpression(dependantIdentifier: RIdentifierExpression?): RAssignmentStatement? {
    if (dependantIdentifier == null) return null

    val usageExpression = dependantIdentifier.parent
    if (usageExpression.elementType != R_MEMBER_EXPRESSION) return null

    return usageExpression.firstChild.reference?.resolve() as RAssignmentStatement
  }

  /**
   * @param objectInstantiationCall RAssignmentStatement expression `obj <- MyClass$new()`
   * @return class definition expression `MyClass <- R6Class("MyClass", list( someField = 0))`
   */
  private fun getClassDefinitionExpression(objectInstantiationCall: RAssignmentStatement?): RAssignmentStatement? {
    if (objectInstantiationCall == null) return null

    val objectCreationCall = objectInstantiationCall.lastChild // MyClass$new()
    val classElement = objectCreationCall?.firstChild?.firstChild // MyClass

    return classElement?.reference?.resolve() as? RAssignmentStatement
  }

  /**
   * @param classDefinition class definition expression `MyClass <- R6Class("MyClass", list( someField = 0))`
   * @return argument info containing all internal members of class
   */
  private fun getClassDefinitionArgumentInfo(classDefinition: RAssignmentStatement?) : RArgumentInfo? {
    if (classDefinition == null) return null

    val r6ClassCall = classDefinition.children?.last() as RCallExpression
    return RParameterInfoUtil.getArgumentInfo(r6ClassCall)
  }

  private fun extractNamedArgumentByName(elementName: String, namedArguments: List<RNamedArgument?>?) : RPsiElement? {
    namedArguments?.forEach {
      if (it != null) {
        if (it.name == elementName) return it
      }
    }

    return null
  }
}