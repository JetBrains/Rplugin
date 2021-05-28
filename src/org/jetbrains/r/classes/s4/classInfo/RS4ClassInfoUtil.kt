/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.s4.classInfo

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.Processor
import org.jetbrains.r.classes.s4.RS4Util
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.packages.RPackageProjectManager
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.isFunctionFromLibrary
import org.jetbrains.r.psi.isFunctionFromLibrarySoft
import org.jetbrains.r.psi.references.RSearchScopeUtil
import org.jetbrains.r.psi.stubs.RS4ClassNameIndex
import org.jetbrains.r.skeleton.psi.RSkeletonCallExpression

object RS4ClassInfoUtil {

  fun getAssociatedClassName(callExpression: RCallExpression,
                             argumentInfo: RArgumentInfo? = RArgumentInfo.getArgumentInfo(callExpression)): String? {
    return getAssociatedClassNameIdentifier(callExpression, argumentInfo)?.name
  }

  fun getAssociatedClassNameIdentifier(callExpression: RCallExpression,
                                       argumentInfo: RArgumentInfo? = RArgumentInfo.getArgumentInfo(callExpression)
  ): RStringLiteralExpression? {
    argumentInfo ?: return null
    if (!callExpression.isFunctionFromLibrarySoft("setClass", "methods") &&
        !callExpression.isFunctionFromLibrarySoft("new", "methods")) return null
    return argumentInfo.getArgumentPassedToParameter("Class") as? RStringLiteralExpression
  }

  fun findSlotInClassDefinition(setClass: RCallExpression, slotName: String): Pair<RS4ClassSlot, RExpression>? {
    if (!setClass.isFunctionFromLibrarySoft("setClass", "methods")) return null
    val argumentInfo = RArgumentInfo.getArgumentInfo(setClass, setClass.project.setClassDefinition) ?: return null
    val className = getAssociatedClassName(setClass, argumentInfo) ?: return null
    var res: Pair<RS4ClassSlot, RExpression>? = null
    val slotProcessor = Processor<Pair<RS4ClassSlot, RExpression>> { slot ->
      if (slot.first.name == slotName) {
        res = slot
        false
      }
      else true
    }
    processSlotsArgument(className, argumentInfo.getArgumentPassedToParameter("slots"), slotProcessor)
    if (res != null) return res
    processRepresentationArgument(className, argumentInfo.getArgumentPassedToParameter("representation"), slotProcessor)
    return res
  }

  /**
   * @param callExpression `setClass` definition expression
   * @return all slots associated with class including slots from superclasses
   */
  fun getAllAssociatedSlots(callExpression: RCallExpression?): List<RS4ClassSlot> {
    if (callExpression == null) return emptyList()
    if (callExpression is RSkeletonCallExpression) {
      // S4 classes from packages and so contains all slots
      return callExpression.associatedS4ClassInfo.slots
    }
    if (!callExpression.isFunctionFromLibrary("setClass", "methods")) return emptyList()
    return CachedValuesManager.getProjectPsiDependentCache(callExpression) {
      val info = callExpression.associatedS4ClassInfo
      if (info == null) return@getProjectPsiDependentCache emptyList<RS4ClassSlot>()
      calcAllAssociatedSlots(callExpression, info)
    }
  }

  /**
   * @param callExpression `setClass` definition expression
   * @return all superclasses associated with class including superclasses from superclasses
   */
  fun getAllAssociatedSuperClasses(callExpression: RCallExpression?): List<RS4SuperClass> {
    if (callExpression == null) return emptyList()
    if (callExpression is RSkeletonCallExpression) {
      // S4 classes from packages and so contains all super classes
      return callExpression.associatedS4ClassInfo.superClasses
    }
    if (!callExpression.isFunctionFromLibrary("setClass", "methods")) return emptyList()
    return CachedValuesManager.getProjectPsiDependentCache(callExpression) {
      val info = callExpression.associatedS4ClassInfo
      if (info == null) return@getProjectPsiDependentCache emptyList<RS4SuperClass>()
      calcAllAssociatedSuperClasses(callExpression, info)
    }
  }

  fun isSubclass(subclass: String, parentClass: String, project: Project, searchScope: GlobalSearchScope): Boolean {
    if (subclass == parentClass || parentClass == "ANY") return true
    val classInfos = RS4ClassNameIndex.findClassInfos(subclass, project, searchScope)
    return classInfos.any { classInfo ->
      val superClasses = classInfo.superClasses.map { it.name }
      if (parentClass in superClasses) return@any true
      val allSuperClasses = calcAllAssociatedSuperClasses(classInfo, project, searchScope).map { it.name }
      parentClass in allSuperClasses
    }
  }

  private fun calcAllAssociatedSlots(callExpression: RCallExpression, s4ClassInfo: RS4ClassInfo): List<RS4ClassSlot> {
    val allSuperClasses = calcAllAssociatedSuperClasses(callExpression, s4ClassInfo)
    val callSearchScope = RSearchScopeUtil.getScope(callExpression)
    val project = callExpression.project
    return (s4ClassInfo.slots + allSuperClasses.flatMap { (superClassName, _) ->
      // Collect slots from super classes and data slot if needed
      RS4ClassNameIndex.findClassInfos(superClassName, project, callSearchScope).flatMap {
        val superClassSlots = it.slots
        when {
          // methods:::.InhSlotNames
          DATA_SLOT_TYPES.containsKey(superClassName) -> superClassSlots + RS4ClassSlot(".Data", DATA_SLOT_TYPES.getValue(superClassName),
                                                                                        superClassName)
          // methods:::.InitSpecialTypesAndClasses
          INDIRECT_ABNORMAL_CLASSES.contains(superClassName) -> superClassSlots + RS4ClassSlot(".xData", superClassName, superClassName)
          else -> superClassSlots
        }
      }
    }).distinctBy { it.name }
  }

  private fun calcAllAssociatedSuperClasses(element: RPsiElement, s4ClassInfo: RS4ClassInfo): List<RS4SuperClass> {
    return calcAllAssociatedSuperClasses(s4ClassInfo,  element.project, RSearchScopeUtil.getScope(element))
  }

  private fun calcAllAssociatedSuperClasses(s4ClassInfo: RS4ClassInfo, project: Project, searchScope: GlobalSearchScope): List<RS4SuperClass> {
    return s4ClassInfo.superClasses.flatMap { superClass ->
      // R keeps all superclasses together in the list, not as a tree
      // So, I don't see any reason to do anything else
      val parentSuperClasses = RS4ClassNameIndex.findClassDefinitions(superClass.name, project, searchScope).flatMap {
        getAllAssociatedSuperClasses(it).map { RS4SuperClass(it.name, false) }
      }
      listOf(superClass) + parentSuperClasses
    }.distinct()
  }

  /**
   * Most likely you need [RCallExpression.getAssociatedS4ClassInfo], not this function
   */
  fun parseS4ClassInfo(callExpression: RCallExpression): RS4ClassInfo? {
    if (!callExpression.isFunctionFromLibrarySoft("setClass", "methods")) return null
    val project = callExpression.project
    val argumentInfo = RArgumentInfo.getArgumentInfo(callExpression, project.setClassDefinition) ?: return null
    val className = getAssociatedClassName(callExpression, argumentInfo) ?: return null
    val representationInfo = parseRepresentationArgument(className, argumentInfo.getArgumentPassedToParameter("representation"))
    val slots = parseSlotsArgument(className, argumentInfo.getArgumentPassedToParameter("slots"))
    val (superClasses, isVirtual) = parseContainsArgument(argumentInfo.getArgumentPassedToParameter("contains"))

    val allSuperClasses = (representationInfo.superClasses + superClasses).distinct()
    val allSlots = (representationInfo.slots + slots).distinctBy { it.name }

    val packageName = RPackageProjectManager.getInstance(project).getProjectPackageDescriptionInfo()?.packageName ?: ""
    return RS4ClassInfo(className, packageName, allSlots, allSuperClasses, isVirtual || representationInfo.isVirtual)
  }

  private val Project.setClassDefinition: RAssignmentStatement
    get() = RS4Util.run { getProjectCachedAssignment(SET_CLASS_DEFINITION_KEY, SET_CLASS_DEFINITION) }

  private fun parseSlotsArgument(className: String, expr: RExpression?): List<RS4ClassSlot> {
    val slots = mutableListOf<RS4ClassSlot>()
    processSlotsArgument(className, expr) { (slot, _) -> slots.add(slot) }
    return slots
  }

  private fun processSlotsArgument(className: String, expr: RExpression?, processor: Processor<Pair<RS4ClassSlot, RExpression>>) {
    if (expr == null || expr is REmptyExpression) return
    val argumentList = RS4Util.parseCharacterVector(expr) ?: return
    for (arg in argumentList) {
      val slots = when (arg) {
        is RNamedArgument -> arg.toComplexSlots(className)
        is RStringLiteralExpression -> arg.toSlot(className)?.let { listOf(it) } ?: emptyList()
        else -> {
          // Most likely something strange and difficult to analyse statically
          emptyList()
        }
      }
      for (slot in slots) {
        if (!processor.process(slot to arg)) return
      }
    }
  }

  private fun parseContainsArgument(expr: RExpression?): Pair<List<RS4SuperClass>, Boolean> {
    if (expr == null || expr is REmptyExpression) return emptyList<RS4SuperClass>() to false

    // Also `class representation` objects vector is suitable.
    // This is some rare use case which difficult to analyse statically
    val argumentList = RS4Util.parseCharacterVector(expr) ?: return emptyList<RS4SuperClass>() to false
    val contains = mutableListOf<RS4SuperClass>()
    var isVirtual = false
    argumentList.forEach { arg ->
      when (arg) {
        is RNamedArgument -> (arg.assignedValue as? RStringLiteralExpression)?.name?.let { contains.add(RS4SuperClass(it, true)) }
        is RStringLiteralExpression -> {
          val name = arg.name?.takeIf { it.isNotEmpty() } ?: return@forEach
          if (name == "VIRTUAL") isVirtual = true
          else contains.add(RS4SuperClass(name, true))
        }
        else -> {
          // Most likely something strange and difficult to analyse statically
        }
      }
    }
    return contains to isVirtual
  }

  private fun parseRepresentationArgument(className: String, expr: RExpression?): RS4ClassInfo {
    val slots = mutableListOf<RS4ClassSlot>()
    val superClasses = mutableListOf<RS4SuperClass>()
    var isVirtual = false
    processRepresentationArgument(className, expr, { (slot, _) -> slots.add(slot) }) { (superClass, _) ->
      if (superClass.name == "VIRTUAL") isVirtual = true
      else superClasses.add(superClass)
      true
    }
    return RS4ClassInfo("", "", slots, superClasses, isVirtual)
  }

  private fun processRepresentationArgument(className: String,
                                            expr: RExpression?,
                                            slotProcessor: Processor<Pair<RS4ClassSlot, RExpression>> = Processor { true },
                                            superClassProcessor: Processor<Pair<RS4SuperClass, RStringLiteralExpression>> = Processor { true }) {
    if (expr == null || expr is REmptyExpression) return
    val argumentList = when (expr) {
      is RCallExpression -> {
        // Any function that returns a `list` is suitable.
        // Arbitrary function is some rare use case which difficult to analyse statically
        if (expr.isFunctionFromLibrarySoft("representation", "methods") ||
            expr.isFunctionFromLibrarySoft("list", "base") ||
            expr.isFunctionFromLibrarySoft("c", "base")) {
          expr.argumentList.expressionList
        }
        else return
      }
      is RStringLiteralExpression -> listOf(expr)
      else -> return
    }
    argumentList.forEach { arg ->
      when (arg) {
        is RNamedArgument -> arg.toSlot(className)?.let { if (!slotProcessor.process(it to arg)) return }
        is RStringLiteralExpression -> {
          val name = arg.name?.takeIf { it.isNotEmpty() } ?: return@forEach
          if (!superClassProcessor.process(RS4SuperClass(name, true) to arg)) return
        }
      }
    }
  }

  fun RNamedArgument.toComplexSlots(className: String): List<RS4ClassSlot> {
    val namePrefix = name.takeIf { it.isNotEmpty() } ?: return emptyList()
    val types = assignedValue?.let { RS4Util.parseCharacterVector(it) } ?: return emptyList()
    return types.mapIndexed { ind, expr ->
      when (expr) {
        is RNamedArgument -> {
          val suffix = expr.name
          val type = expr.assignedValue?.toType() ?: ""
          RS4ClassSlot("$namePrefix.$suffix", type, className)
        }
        else -> {
          val type = expr.toType() ?: ""
          val name = if (types.size == 1) namePrefix else "$namePrefix${ind + 1}"
          RS4ClassSlot(name, type, className)
        }
      }
    }
  }

  fun RNamedArgument.toSlot(className: String): RS4ClassSlot? {
    val name = name.takeIf { it.isNotEmpty() } ?: return null
    val type = when (val typeExpr = assignedValue) {
                 is RCallExpression -> RS4Util.parseCharacterVector(typeExpr)?.firstOrNull()?.toType()
                 else -> typeExpr?.toType()
               } ?: ""
    return RS4ClassSlot(name, type, className)
  }

  fun RStringLiteralExpression.toSlot(className: String): RS4ClassSlot? {
    val name = name?.takeIf { it.isNotEmpty() } ?: return null
    return RS4ClassSlot(name, "ANY", className)
  }

  private fun RPsiElement.toType(): String? {
    return when (this) {
      is RStringLiteralExpression -> name
      is RNaLiteral -> "NA"
      is RNullLiteral -> "NULL"
      else -> null
    }
  }

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

  private val SET_CLASS_DEFINITION =
    """setClass <- function (Class, representation = list(), prototype = NULL, contains = character(),
                             validity = NULL, access = list(), where = topenv(parent.frame()),
                             version = .newExternalptr(), sealed = FALSE, package = getPackageName(where),
                             S3methods = FALSE, slots) {}""".trimIndent()

  private val SET_CLASS_DEFINITION_KEY: Key<RAssignmentStatement> = Key.create("S4_SET_CLASS_DEFINITION")
}