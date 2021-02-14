/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.r6

import com.intellij.openapi.util.Key
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil
import org.jetbrains.r.packages.RPackageProjectManager
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.impl.RCallExpressionImpl
import org.jetbrains.r.psi.isFunctionFromLibrarySoft

object R6ClassInfoUtil {
  public const val R6PackageName = "R6"
  public const val R6CreateClassMethod = "R6Class"

  private const val argumentClassName = "classname"
  private const val argumentSuperClass = "inherit"
  private const val argumentPublic = "public"
  private const val argumentPrivate = "private"

  private val INSTANTIATE_CLASS_DEFINITION_KEY: Key<RAssignmentStatement> = Key.create("R6_INSTANTIATE_CLASS_DEFINITION")

  private val INSTANTIATE_CLASS_DEFINITION =
    """R6Class <- function (classname = NULL, public = list(), private = NULL,
                            active = NULL, inherit = NULL, lock_objects = TRUE, class = TRUE,
                            portable = TRUE, lock_class = FALSE, cloneable = TRUE,
                            parent_env = parent.frame(), lock) {}""".trimIndent()

  fun getAssociatedClassName(callExpression: RCallExpression,
                             argumentInfo: RArgumentInfo? = RParameterInfoUtil.getArgumentInfo(callExpression)): String? {
    argumentInfo ?: return null
    if (!callExpression.isFunctionFromLibrarySoft(R6CreateClassMethod, R6PackageName)) return null
    val arg = argumentInfo.getArgumentPassedToParameter(argumentClassName) as? RStringLiteralExpression
    return arg?.name
  }

  fun getAssociatedSuperClassName(callExpression: RCallExpression,
                                  argumentInfo: RArgumentInfo? = RParameterInfoUtil.getArgumentInfo(callExpression)): String? {
    argumentInfo ?: return null
    if (!callExpression.isFunctionFromLibrarySoft(R6CreateClassMethod, R6PackageName)) return null
    return (argumentInfo.getArgumentPassedToParameter(argumentSuperClass) as? RIdentifierExpression)?.name
  }

  fun getAssociatedFields(callExpression: RCallExpression,
                          argumentInfo: RArgumentInfo? = RParameterInfoUtil.getArgumentInfo(callExpression)): List<R6ClassField>? {
    argumentInfo ?: return null
    if (!callExpression.isFunctionFromLibrarySoft(R6CreateClassMethod, R6PackageName)) return null

    var r6ClassFields = mutableListOf<R6ClassField>()
    val publicContents = (argumentInfo.getArgumentPassedToParameter(argumentPublic) as? RCallExpressionImpl)?.argumentList?.expressionList
    val privateContents = (argumentInfo.getArgumentPassedToParameter(argumentPrivate) as? RCallExpressionImpl)?.argumentList?.expressionList

    if (!publicContents.isNullOrEmpty()) getFieldsFromExpressionList(r6ClassFields, publicContents, true)
    if (!privateContents.isNullOrEmpty()) getFieldsFromExpressionList(r6ClassFields, privateContents, false)

    return r6ClassFields
  }

  fun getAssociatedMethods(callExpression: RCallExpression,
                           argumentInfo: RArgumentInfo? = RParameterInfoUtil.getArgumentInfo(callExpression)): List<R6ClassMethod>? {
    argumentInfo ?: return null
    if (!callExpression.isFunctionFromLibrarySoft(R6CreateClassMethod, R6PackageName)) return null
    return emptyList()
  }

  fun getAssociatedActiveBindings(callExpression: RCallExpression,
                                  argumentInfo: RArgumentInfo? = RParameterInfoUtil.getArgumentInfo(callExpression)): List<R6ClassActiveBinding>? {
    argumentInfo ?: return null
    if (!callExpression.isFunctionFromLibrarySoft(R6CreateClassMethod, R6PackageName)) return null
    return emptyList()
  }

  fun parseR6ClassInfo(callExpression: RCallExpression): R6ClassInfo? {
    if (!callExpression.isFunctionFromLibrarySoft(R6CreateClassMethod, R6PackageName)) return null
    val project = callExpression.project
    var definition = project.getUserData(INSTANTIATE_CLASS_DEFINITION_KEY)

    if (definition == null || !definition.isValid) {
      val instantiateClassDefinition =
        RElementFactory.createRPsiElementFromText(callExpression.project, INSTANTIATE_CLASS_DEFINITION) as RAssignmentStatement
      definition = instantiateClassDefinition.also { project.putUserData(INSTANTIATE_CLASS_DEFINITION_KEY, it) }
    }

    val argumentInfo = RParameterInfoUtil.getArgumentInfo(callExpression, definition) ?: return null
    val className = getAssociatedClassName(callExpression, argumentInfo) ?: return null
    val superClassName = getAssociatedSuperClassName(callExpression, argumentInfo) ?: ""
    val fields = getAssociatedFields(callExpression, argumentInfo) ?: emptyList()
    val methods = getAssociatedMethods(callExpression, argumentInfo) ?: emptyList()
    val activeBindings = getAssociatedActiveBindings(callExpression, argumentInfo) ?: emptyList()

    val packageName = RPackageProjectManager.getInstance(project).getProjectPackageDescriptionInfo()?.packageName ?: ""
    // return R6ClassInfo(className, packageName, superClassName, fields, methods, activeBindings)
    return R6ClassInfo.createDummyFromCoupleParameters(className, packageName)
  }

  private fun getFieldsFromExpressionList(r6ClassFields: MutableList<R6ClassField>, callExpressions: List<RExpression>, isPublicScope: Boolean){
    callExpressions.forEach {
      var isField = true
      for (child in it.children){
        if (child is RFunctionExpression) {
          isField = false
          break
        }
      }

      if (isField && !it.name.isNullOrEmpty()){
        r6ClassFields.add(R6ClassField(it.name!!, isPublicScope))
      }
    }
  }
}