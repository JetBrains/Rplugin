/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes

import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil
import org.jetbrains.r.packages.RPackageProjectManager
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RExpression
import org.jetbrains.r.psi.api.RNamedArgument
import org.jetbrains.r.psi.api.RStringLiteralExpression
import org.jetbrains.r.psi.isFunctionFromLibrary
import org.jetbrains.r.psi.isFunctionFromLibrarySoft
import org.jetbrains.r.psi.references.RSearchScopeUtil
import org.jetbrains.r.psi.stubs.RS4ClassNameIndex

object RS4ClassInfoUtil {

  fun getAssociatedClassName(callExpression: RCallExpression,
                             argumentInfo: RArgumentInfo? = RParameterInfoUtil.getArgumentInfo(callExpression)): String? {
    argumentInfo ?: return null
    if (!callExpression.isFunctionFromLibrary("setClass", "methods") &&
        !callExpression.isFunctionFromLibrary("new", "methods")) return null
    return (argumentInfo.getArgumentPassedToParameter("Class") as? RStringLiteralExpression)?.name
  }

  /**
   * Most likely you need [RCallExpression.getAssociatedS4ClassInfo], not this function
   */
  fun parseS4ClassInfo(callExpression: RCallExpression): RS4ClassInfo? {
    if (!callExpression.isFunctionFromLibrary("setClass", "methods")) return null
    val argumentInfo = RParameterInfoUtil.getArgumentInfo(callExpression) ?: return null
    val className = getAssociatedClassName(callExpression, argumentInfo) ?: return null
    val representationInfo = parseRepresentationArgument(argumentInfo.getArgumentPassedToParameter("representation"))
    val slots = parseSlotsArgument(argumentInfo.getArgumentPassedToParameter("slots"))
    val (superClasses, isVirtual) = parseContainsArgument(argumentInfo.getArgumentPassedToParameter("contains"))

    val project = callExpression.project
    val callSearchScope = RSearchScopeUtil.getScope(callExpression)
    val allSuperClasses = (representationInfo.superClasses + superClasses).distinct().flatMap { superClassName ->
      // R keeps all superclasses together in the list, not as a tree
      // So, I don't see any reason to do anything else
      val parentSuperClasses = RS4ClassNameIndex.findClassInfos(superClassName, project, callSearchScope).flatMap { it.superClasses }
      listOf(superClassName) + parentSuperClasses
    }.distinct()

    val allSlots = (representationInfo.slots + slots + allSuperClasses.flatMap { superClassName ->
      // Collect slots from super classes and data slot if needed
      RS4ClassNameIndex.findClassInfos(superClassName, project, callSearchScope).flatMap {
        val superClassSlots = it.slots
        when {
          // methods:::.InhSlotNames
          DATA_SLOT_TYPES.containsKey(superClassName) -> superClassSlots + RS4ClassSlot(".Data", DATA_SLOT_TYPES.getValue(superClassName))
          // methods:::.InitSpecialTypesAndClasses
          INDIRECT_ABNORMAL_CLASSES.contains(superClassName) -> superClassSlots + RS4ClassSlot(".xData", superClassName)
          else -> superClassSlots
        }
      }
    }).distinctBy { it.name }

    val packageName = RPackageProjectManager.getInstance(project).getProjectPackageDescriptionInfo()?.packageName ?: ""
    return RS4ClassInfo(className, packageName, allSlots, allSuperClasses, isVirtual || representationInfo.isVirtual)
  }

  private fun parseSlotsArgument(expr: RExpression?): List<RS4ClassSlot> {
    if (expr == null) return emptyList()
    val argumentList = parseCharacterVector(expr) ?: return emptyList()
    val slots = mutableListOf<RS4ClassSlot>()
    argumentList.forEach { arg ->
      when (arg) {
        is RNamedArgument -> arg.toSlot()?.let { slots.add(it) }
        is RStringLiteralExpression -> {
          val name = arg.name?.takeIf { it.isNotEmpty() } ?: return@forEach
          slots.add(RS4ClassSlot(name, "ANY"))
        }
        else -> {
          // Most likely something strange and difficult to analyse statically
        }
      }
    }
    return slots
  }

  private fun parseContainsArgument(expr: RExpression?): Pair<List<String>, Boolean> {
    if (expr == null) return emptyList<String>() to false

    // Also `class representation` objects vector is suitable.
    // This is some rare use case which difficult to analyse statically
    val argumentList = parseCharacterVector(expr) ?: return emptyList<String>() to false
    val contains = mutableListOf<String>()
    var isVirtual = false
    argumentList.forEach { arg ->
      when (arg) {
        is RNamedArgument -> (arg.assignedValue as? RStringLiteralExpression)?.name?.let { contains.add(it) }
        is RStringLiteralExpression -> {
          val name = arg.name?.takeIf { it.isNotEmpty() } ?: return@forEach
          if (name == "VIRTUAL") isVirtual = true
          else contains.add(name)
        }
        else -> {
          // Most likely something strange and difficult to analyse statically
        }
      }
    }
    return contains to isVirtual
  }

  private fun parseRepresentationArgument(expr: RExpression?): RS4ClassInfo {
    if (expr == null) return EMPTY_CLASS_INFO
    val argumentList = when (expr) {
      is RCallExpression -> {
        // Any function that returns a `list` is suitable.
        // Arbitrary function is some rare use case which difficult to analyse statically
        if (expr.isFunctionFromLibrarySoft("representation", "methods")
            || expr.isFunctionFromLibrarySoft("list", "base")) {
          expr.argumentList.expressionList
        }
        else return EMPTY_CLASS_INFO
      }
      is RStringLiteralExpression -> listOf(expr)
      else -> return EMPTY_CLASS_INFO
    }
    val slots = mutableListOf<RS4ClassSlot>()
    val superClasses = mutableListOf<String>()
    var isVirtual = false
    argumentList.forEach { arg ->
      when (arg) {
        is RNamedArgument -> arg.toSlot()?.let { slots.add(it) }
        is RStringLiteralExpression -> {
          val name = arg.name?.takeIf { it.isNotEmpty() } ?: return@forEach
          if (name == "VIRTUAL") isVirtual = true
          else superClasses.add(name)
        }
        else -> {
          // Most likely something strange and difficult to analyse statically
        }
      }
    }
    return RS4ClassInfo("", "", slots, superClasses, isVirtual)
  }

  private fun parseCharacterVector(expr: RExpression): List<RExpression>? = when (expr) {
    is RCallExpression -> {
      // Any function that returns a `vector` of `characters` is suitable.
      // Arbitrary function returns vector is some rare use case which difficult to analyse statically
      if (expr.isFunctionFromLibrarySoft("c", "base") ||
          expr.isFunctionFromLibrarySoft("list", "base")) {
        expr.argumentList.expressionList
      }
      else null
    }
    is RStringLiteralExpression -> listOf(expr)
    else -> null
  }

  private fun RNamedArgument.toSlot(): RS4ClassSlot? {
    val name = name.takeIf { it.isNotEmpty() } ?: return null
    val type = (assignedValue as? RStringLiteralExpression)?.name ?: ""
    return RS4ClassSlot(name, type)
  }

  private val EMPTY_CLASS_INFO = RS4ClassInfo("", "", emptyList(), emptyList(), false)

  // see `methods:::.indirectAbnormalClasses`
  private val INDIRECT_ABNORMAL_CLASSES = listOf("environment", "externalptr", "name", "NULL")

  // map of <class name, type of `.Data` slot after inheritance> (except `VIRTUAL` class)
  // see methods:::.BasicClasses
  // Subclasses of `vector` which are not in BASIC_CLASSES
  private val DATA_SLOT_TYPES = mapOf(
    "(" to "(", "{" to "{", "<-" to "<-", "ANY" to "ANY", "array" to "array", "call" to "call",
    "character" to "character", "className" to "character", "complex" to "complex", "data.frame" to "list",
    "double" to "double", "expression" to "expression", "factor" to "integer", "for" to "for",
    "function" to "function", "if" to "if", "integer" to "integer", "language" to "language", "list" to "list",
    "listOfMethods" to "list", "logical" to "logical", "matrix" to "matrix", "missing" to "missing",
    "mts" to "vector", "namedList" to "list", "numeric" to "numeric", "ObjectsWithPackage" to "character",
    "ordered" to "integer", "raw" to "raw", "repeat" to "repeat", "S3" to "S3", "S4" to "S4",
    "signature" to "character", "stampedEnv" to "list", "structure" to "vector", "ts" to "vector",
    "vector" to "vector", "while" to "while"
  )
}