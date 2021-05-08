/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.r6

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil
import org.jetbrains.r.parsing.RElementTypes.*
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.impl.RCallExpressionImpl
import org.jetbrains.r.psi.isFunctionFromLibrarySoft
import org.jetbrains.r.psi.references.RSearchScopeUtil
import org.jetbrains.r.psi.stubs.classes.R6ClassNameIndex

object R6ClassPsiUtil {

  /**
   * @param dependantIdentifier `someMember` psi-element of expression `obj$someMember`
   * @return RPsiElement with name of `someMember`
   */
  fun getSearchedIdentifier(dependantIdentifier: RIdentifierExpression?) : RPsiElement? {
    if (dependantIdentifier == null) return null

    val classDefinitionCall = getClassDefinitionCallFromMemberUsage(dependantIdentifier) ?: return null
    val className = R6ClassInfoUtil.getAssociatedClassNameFromR6ClassCall(classDefinitionCall) ?: return null
    val classNamesHierarchy = R6ClassInfoUtil.getAssociatedSuperClassesHierarchy(classDefinitionCall)
    classNamesHierarchy?.add(0, className)

    val callSearchScope = RSearchScopeUtil.getScope(classDefinitionCall)
    val project = classDefinitionCall.project

    val r6ClassInfo = run findMemberDefinition@ {
      (classNamesHierarchy)?.reversed()?.forEach {
        val r6ClassInfo = R6ClassNameIndex.findClassInfos(it, project, callSearchScope).firstOrNull()

        if (r6ClassInfo != null) {
          if (r6ClassInfo.containsMember(dependantIdentifier.name)) return@findMemberDefinition r6ClassInfo
        }
      }
    } as R6ClassInfo?

    r6ClassInfo ?: return null
    val r6ClassDefinitionCall = R6ClassNameIndex.findClassDefinitions(r6ClassInfo.className, project, callSearchScope).firstOrNull()
    val argumentInfo = getClassDefinitionArgumentInfo(r6ClassDefinitionCall) ?: return null

    val publicMembers = getClassMemberExpressionsOfArgument(argumentInfo, R6ClassInfoUtil.argumentPublic)
    val privateMembers = getClassMemberExpressionsOfArgument(argumentInfo, R6ClassInfoUtil.argumentPrivate)
    val activeMembers = getClassMemberExpressionsOfArgument(argumentInfo, R6ClassInfoUtil.argumentActive)

    return extractNamedArgumentByName(dependantIdentifier.name, publicMembers?.mapNotNull { it as? RNamedArgument })
           ?: extractNamedArgumentByName(dependantIdentifier.name, privateMembers?.mapNotNull { it as? RNamedArgument })
           ?: extractNamedArgumentByName(dependantIdentifier.name, activeMembers?.mapNotNull { it as? RNamedArgument })
  }

  /**
   * @param dependantIdentifier `someMember` psi-element of expression `obj$someMember`
   * @param objectInstantiationCall RAssignmentStatement expression `obj <- MyClass$new()`
   * @return class definition expression `MyClass <- R6Class("MyClass", list( someField = 0))`
   */
  private fun getClassDefinitionExpression(dependantIdentifier: RIdentifierExpression?, objectInstantiationCall: RAssignmentStatement?): RAssignmentStatement? {
    if (dependantIdentifier == null) return null

    // handling search request from inside of class usage with `self$field`
    if (objectInstantiationCall == null) {
      if (dependantIdentifier.parent?.firstChild?.text == R6ClassInfoUtil.R6ClassThisKeyword){
        var currentParent = dependantIdentifier.parent
        var currentCall = currentParent as? RCallExpression

        while (dependantIdentifier.parent != null){
          if (currentCall?.isFunctionFromLibrarySoft(R6ClassInfoUtil.R6CreateClassMethod, R6ClassInfoUtil.R6PackageName) == true) break
          currentParent = currentParent.parent
          currentCall = currentParent as? RCallExpression
        }

        return currentCall?.parent as? RAssignmentStatement
      }

      return null
    }

    // handling search request from classic out-of-class-definition usage
    val objectCreationCall = objectInstantiationCall.lastChild // MyClass$new()
    val classElement = objectCreationCall?.firstChild?.firstChild // MyClass

    return classElement?.reference?.resolve() as? RAssignmentStatement
  }

  /**
   * @param rIdentifierExpression `someMember` of expression like `classObject$someMember` or `self$someMember`
   * @return `R6Class` function call, which defines class containing `someMember`
   */
  fun getClassDefinitionCallFromMemberUsage(rIdentifierExpression: RIdentifierExpression?) : RCallExpression? {
    if (rIdentifierExpression == null) return null
    val usedClassVariable = getClassIdentifierFromChainedUsages(rIdentifierExpression.parent as? RMemberExpression)
    return getClassDefinitionFromClassVariableUsage(usedClassVariable)
  }

  /**
   * @param rMemberExpression expression like `classObject$someMember` or `self$someMember`
   * @return `R6Class` function call, which defines class containing `someMember`
   */
  fun getClassDefinitionCallFromMemberUsage(rMemberExpression: RMemberExpression?) : RCallExpression? {
    if (rMemberExpression == null) return null
    val classObject = getClassIdentifierFromChainedUsages(rMemberExpression)
    return getClassDefinitionFromClassVariableUsage(classObject)
  }

  private fun getClassDefinitionFromClassVariableUsage(classObject: PsiElement?) : RCallExpression? {
    // `self$someMember`
    if (classObject?.text == R6ClassInfoUtil.R6ClassThisKeyword) {
      val parentFunction = PsiTreeUtil.getStubOrPsiParentOfType(classObject, RCallExpression::class.java)
      val r6ClassDefinitionCall = PsiTreeUtil.getStubOrPsiParentOfType(parentFunction, RCallExpression::class.java)

      if (r6ClassDefinitionCall?.isFunctionFromLibrarySoft(R6ClassInfoUtil.R6CreateClassMethod, R6ClassInfoUtil.R6PackageName) == true) {
        return r6ClassDefinitionCall
      }
    }
    // `classObject$someMember`
    else {
      // `classObject <- MyClass$new()` from `classObject$someMember$someMethod()$someMethod2()$someMember`
      val r6ObjectCreationExpression = classObject?.reference?.resolve() as? RAssignmentStatement
      // `MyClass` from `classObject <- MyClass$new()`
      val usedClassVariable = r6ObjectCreationExpression?.assignedValue?.firstChild?.firstChild
      // `MyClass <- R6Class(...)` from `MyClass`
      val classDefinitionAssignment = usedClassVariable?.reference?.resolve() as? RAssignmentStatement

      return classDefinitionAssignment?.assignedValue as? RCallExpression
    }

    return null
  }

  /**
   * @param classDefinitionAssignment class definition expression `MyClass <- R6Class("MyClass", list( someField = 0))`
   * @return argument info containing all internal members of class
   */
  fun getClassDefinitionArgumentInfo(classDefinitionAssignment: RAssignmentStatement?) : RArgumentInfo? {
    if (classDefinitionAssignment == null) return null
    val classDefinitionCall = classDefinitionAssignment.children.last() as? RCallExpression
    return getClassDefinitionArgumentInfo(classDefinitionCall)
  }

  /**
   * @param classDefinitionCall class definition expression `R6Class("MyClass", list( someField = 0))`
   * @return argument info containing all internal members of class
   */
  fun getClassDefinitionArgumentInfo(classDefinitionCall: RCallExpression?) : RArgumentInfo? {
    if (classDefinitionCall == null) return null
    return RParameterInfoUtil.getArgumentInfo(classDefinitionCall)
  }

  /**
   * @param argumentInfo information about arguments of R6 class definition call
   * @param argumentName name of argument (i.e. `public`, `private`, `active`) from where to pick class members
   */
  private fun getClassMemberExpressionsOfArgument(argumentInfo: RArgumentInfo, argumentName: String) : List<RExpression>? {
    val members = argumentInfo.getArgumentPassedToParameter(argumentName) as? RCallExpressionImpl
    return members?.argumentList?.expressionList
  }

  /**
   * @param dependantIdentifier `someMember` psi-element of expression `classObject$someMember$someMethod()$someActive$someMethod2()`
   * @return RIdentifier of R6-class object
   */
  private fun getR6ObjectIdentifierFromChainedUsage(dependantIdentifier: RIdentifierExpression?): RIdentifierExpression? {
    if (dependantIdentifier == null) return null

    val usageExpression = dependantIdentifier.parent
    if (usageExpression.elementType != R_MEMBER_EXPRESSION) return null

    var r6Object = usageExpression.firstChild
    while (r6Object != null && r6Object.elementType != R_IDENTIFIER_EXPRESSION){
      r6Object = r6Object.firstChild
    }

    return r6Object as RIdentifierExpression
  }

  /**
   * @param rMemberExpression `classObject$someMember$someMethod()$someActive()`
   * @return `classObject` identifier as the most left psi-element in chained usage expression
   */
  private fun getClassIdentifierFromChainedUsages(rMemberExpression: RMemberExpression?) : RIdentifierExpression? {
    if (rMemberExpression == null) return null
    val classIdentifier = PsiTreeUtil.firstChild(rMemberExpression).parent as? RIdentifierExpression
    if (classIdentifier?.parent.elementType != R_MEMBER_EXPRESSION) return null
    return classIdentifier
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