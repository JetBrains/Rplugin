/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.BasicInsertHandler
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.ClassConditionKey
import com.intellij.openapi.util.TextRange
import icons.RIcons
import org.jetbrains.r.classes.s4.methods.RS4MethodsUtil.associatedS4GenericInfo
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionHttpResponse
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionUtils.priority
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.packages.RPackage
import org.jetbrains.r.psi.TableColumnInfo
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.refactoring.RNamesValidator
import javax.swing.Icon
import kotlin.math.min

const val ARGUMENT_VALUE_PRIORITY = 140.0
const val LIBRARY_METHOD_PRIORITY = 120.0
const val TABLE_MANIPULATION_PRIORITY = 110.0
const val IMPORT_PACKAGE_PRIORITY = 110.0
const val SLOT_NAME_PRIORITY = 110.0
const val NAMED_ARGUMENT_PRIORITY = 100.0
const val LOADED_S4_CLASS_NAME = 100.0
const val VARIABLE_GROUPING = 90
const val NOT_LOADED_S4_CLASS_NAME = 50.0
const val LANGUAGE_S4_CLASS_NAME = 25.0
const val PACKAGE_PRIORITY = -1.0
const val GLOBAL_GROUPING = 0
const val NAMESPACE_NAME_GROUPING = -1

class RLookupElement(val lookup: String,
                     private val bold: Boolean,
                     private val icon: Icon? = null,
                     val packageName: String? = null,
                     private val tailText: String? = null,
                     private val itemText: String = lookup) : LookupElement() {

  override fun getLookupString() = lookup

  override fun renderElement(presentation: LookupElementPresentation) {
    presentation.itemText = itemText
    presentation.isItemTextBold = bold
    presentation.icon = icon
    presentation.typeText = packageName
    if (tailText != null) presentation.appendTailText(tailText, true)
  }
}

data class TableManipulationColumnLookup(val column: TableColumnInfo) {
  override fun equals(other: Any?): Boolean {
    if (other !is TableManipulationColumnLookup) return false
    return other.quotedNameIfNeeded == quotedNameIfNeeded && other.column.type == column.type
  }

  override fun hashCode(): Int {
    return quotedNameIfNeeded.hashCode() * 31 + column.type.hashCode()
  }

  private val quotedNameIfNeeded
    get() =  if (column.quoteNeeded) "\"${column.name}\"" else column.name
}

interface RLookupElementInsertHandler {
  fun getInsertHandlerForFunctionCall(functionParameters: String): InsertHandler<LookupElement> = BasicInsertHandler<LookupElement>()
  fun getInsertHandlerForAssignment(assignment: RAssignmentStatement): InsertHandler<LookupElement>
}

class REmptyLookupElementInsertHandler : RLookupElementInsertHandler {
  override fun getInsertHandlerForAssignment(assignment: RAssignmentStatement) = BasicInsertHandler<LookupElement>()
}

sealed class MachineLearningCompletionLookupDecorator(delegate: LookupElement) : LookupElementDecorator<LookupElement>(delegate) {
  companion object {
    val CLASS_CONDITION_KEY: ClassConditionKey<MachineLearningCompletionLookupDecorator> =
      ClassConditionKey.create(MachineLearningCompletionLookupDecorator::class.java)
  }

  class New(delegate: LookupElement) : MachineLearningCompletionLookupDecorator(delegate)
  class Merged(delegate: LookupElement) : MachineLearningCompletionLookupDecorator(delegate)
}

class RLookupElementFactory(private val functionInsertHandler: RLookupElementInsertHandler = REmptyLookupElementInsertHandler(),
                            private val constantInsertHandler: RLookupElementInsertHandler = REmptyLookupElementInsertHandler()) {
  fun createGlobalLookupElement(assignment: RAssignmentStatement): LookupElement {
    return if (assignment.isFunctionDeclaration) {
      createFunctionLookupElement(assignment)
    }
    else {
      val name = assignment.name
      val packageName = RPackage.getOrCreateRPackageBySkeletonFile(assignment.containingFile)?.name
      val icon = AllIcons.Nodes.Constant
      createLookupElementWithGrouping(RLookupElement(name, false, icon, packageName),
                                      constantInsertHandler.getInsertHandlerForAssignment(assignment), GLOBAL_GROUPING)
    }
  }

  fun createFunctionLookupElement(functionAssignment: RAssignmentStatement, isLocal: Boolean = false): LookupElement {
    return if (functionAssignment.name.startsWith("%")) createOperatorLookupElement(functionAssignment, isLocal)
    else createFunctionLookupElement(functionAssignment.name, AllIcons.Nodes.Function,
                                     functionAssignment.functionParameters, functionAssignment, isLocal)
  }

  fun createS4GenericLookupElement(genericExpression: RS4GenericOrMethodHolder): LookupElement {
    val functionParameters =
      when (genericExpression) {
        is RAssignmentStatement -> genericExpression.functionParameters
        is RCallExpression -> {
          when (val def = RArgumentInfo.getArgumentByName(genericExpression, "def")) {
            is RFunctionExpression -> def.parameterList?.text
            is RIdentifierExpression -> (def.reference.resolve() as? RAssignmentStatement)?.functionParameters
            else -> null
          }
        }
        else -> null
      } ?: ""
    val name = genericExpression.associatedS4GenericInfo!!.methodName
    return createFunctionLookupElement(name, AllIcons.Nodes.Method, functionParameters, genericExpression, genericExpression is RCallExpression)
  }

  private fun createFunctionLookupElement(name: String, icon: Icon, functionParameters: String, def: RPsiElement, isLocal: Boolean = false): LookupElement {
    val packageName = if (isLocal) null else RPackage.getOrCreateRPackageBySkeletonFile(def.containingFile)?.name
    return createLookupElementWithGrouping(RLookupElement(name, false, icon, packageName, functionParameters),
                                           functionInsertHandler.getInsertHandlerForFunctionCall(functionParameters),
                                           if (isLocal) VARIABLE_GROUPING else GLOBAL_GROUPING)
  }

  fun createNamespaceAccess(lookupString: String): LookupElement {
    val insertHandler = InsertHandler<LookupElement> { context, _ ->
      val document = context.document
      document.replaceString(context.startOffset, context.tailOffset, RNamesValidator.quoteIfNeeded(lookupString))
      context.editor.caretModel.moveToOffset(context.tailOffset)
    }
    return createLookupElementWithGrouping(RLookupElement(lookupString, false, AllIcons.Nodes.Package),
                                           insertHandler, NAMESPACE_NAME_GROUPING)
  }

  fun createAtAccess(lookupString: String, type: String = ""): LookupElement {
    val insertHandler = InsertHandler<LookupElement> { context, _ ->
      val document = context.document
      document.replaceString(context.startOffset, context.tailOffset, RNamesValidator.quoteIfNeeded(lookupString))
      context.editor.caretModel.moveToOffset(context.tailOffset)
    }
    return createLookupElementWithPriority(RLookupElement(lookupString, true, AllIcons.Nodes.Field, type),
                                           insertHandler, SLOT_NAME_PRIORITY)
  }

  fun createLocalVariableLookupElement(lookupString: String, isParameter: Boolean): LookupElement {
    val icon = if (isParameter) AllIcons.Nodes.Parameter else AllIcons.Nodes.Variable
    return PrioritizedLookupElement.withGrouping(RLookupElement(lookupString, true, icon), VARIABLE_GROUPING)
  }

  fun createPackageLookupElement(lookupString: String, inImport: Boolean): LookupElement {
    return if (inImport) {
      PrioritizedLookupElement.withPriority(RLookupElement(lookupString, true, AllIcons.Nodes.Package), IMPORT_PACKAGE_PRIORITY)
    }
    else {
      val insertHandler = InsertHandler<LookupElement> { context, _ ->
        val document = context.document
        document.insertString(context.tailOffset, "::")
        AutoPopupController.getInstance(context.project).scheduleAutoPopup(context.editor)
        context.editor.caretModel.moveCaretRelatively(2, 0, false, false, false)
      }
      createLookupElementWithPriority(RLookupElement(lookupString, true, AllIcons.Nodes.Package, tailText = "::"),
                                      insertHandler, PACKAGE_PRIORITY)
    }
  }

  fun createMemberLookupElement(lookupString: String, priority: Double): LookupElement {
    return PrioritizedLookupElement.withPriority(RLookupElement(lookupString, true, AllIcons.Nodes.Field), priority)
  }

  fun createLibraryFunctionLookupElement(lookupString: String, priority: Double = LIBRARY_METHOD_PRIORITY): LookupElement {
    return PrioritizedLookupElement.withPriority(RLookupElement(lookupString, true, AllIcons.Nodes.Function), priority)
  }

  fun createQuotedLookupElement(lookupString: String,
                                priority: Double,
                                bold: Boolean,
                                icon: Icon? = null,
                                packageName: String? = null,
                                tailText: String? = null): LookupElement {
    return createLookupElementWithPriority(
      RLookupElement(lookupString, bold, icon, packageName = packageName, itemText = "\"$lookupString\"", tailText = tailText),
      QUOTE_INSERT_HANDLER, priority
    )
  }

  fun createMachineLearningCompletionLookupElement(variant: MachineLearningCompletionHttpResponse.CompletionVariant): LookupElement {
    val element = RLookupElement(variant.text, true, RIcons.MachineLearning)
    val prioritized = createLookupElementWithPriority(element, BasicInsertHandler(), variant.score)
    return MachineLearningCompletionLookupDecorator.New(prioritized)
  }

  fun createMergedMachineLearningCompletionLookupElement(lookupElement: LookupElement,
                                                         mlVariant: MachineLearningCompletionHttpResponse.CompletionVariant): LookupElement {
    val mlPriority = mlVariant.score
    val priority = lookupElement.priority?.let { maxOf(it, mlPriority) } ?: mlPriority
    val prioritized = PrioritizedLookupElement.withPriority(lookupElement, priority)
    lookupElement.copyUserDataTo(prioritized)
    return MachineLearningCompletionLookupDecorator.Merged(prioritized)
  }

  private fun createOperatorLookupElement(functionAssignment: RAssignmentStatement, isLocal: Boolean): LookupElement {
    val packageName = if (isLocal) null else RPackage.getOrCreateRPackageBySkeletonFile(functionAssignment.containingFile)?.name
    val icon = AllIcons.Nodes.Function
    val insertHandler = InsertHandler<LookupElement> { context, _ ->
      val document = context.document
      val startOffset = context.tailOffset
      val endOffset = min(context.tailOffset + 1, document.textLength)
      if (endOffset <= startOffset) return@InsertHandler
      if (document.getText(TextRange(startOffset, endOffset)) == "%") {
        document.replaceString(startOffset, endOffset, "")
      }
    }
    return createLookupElementWithGrouping(RLookupElement(functionAssignment.name, false, icon, packageName),
                                           insertHandler, if (isLocal) VARIABLE_GROUPING else GLOBAL_GROUPING)
  }

  companion object {
    fun createLookupElementWithGrouping(lookupElement: LookupElement,
                                        insertHandler: InsertHandler<LookupElement>,
                                        grouping: Int): LookupElement {
      val lookupElementWithInsertHandler = PrioritizedLookupElement.withInsertHandler(lookupElement, insertHandler)
      return PrioritizedLookupElement.withGrouping(lookupElementWithInsertHandler, grouping)
    }

    fun createLookupElementWithPriority(lookupElement: LookupElement,
                                        insertHandler: InsertHandler<LookupElement>,
                                        priority: Double): LookupElement {
      val lookupElementWithInsertHandler = PrioritizedLookupElement.withInsertHandler(lookupElement, insertHandler)
      return PrioritizedLookupElement.withPriority(lookupElementWithInsertHandler, priority)
    }

    fun createNamedArgumentLookupElement(lookupString: String,
                                         packageName: String? = null,
                                         priority: Double = NAMED_ARGUMENT_PRIORITY): LookupElement {
      val icon = AllIcons.Nodes.Parameter
      val insertHandler = InsertHandler<LookupElement> { context, _ ->
        val document = context.document
        document.insertString(context.tailOffset, " = ")
        context.editor.caretModel.moveCaretRelatively(3, 0, false, false, false)
      }
      return createLookupElementWithPriority(RLookupElement(lookupString, true, icon, packageName = packageName, tailText = " = "),
                                             insertHandler, priority)
    }

    private val QUOTE_INSERT_HANDLER = InsertHandler<LookupElement> { insertHandlerContext, _ ->
      val document = insertHandlerContext.document
      val startOffset = insertHandlerContext.startOffset
      val tailOffset = insertHandlerContext.tailOffset
      document.insertString(startOffset, "\"")
      document.insertString(tailOffset + 1, "\"")
      insertHandlerContext.editor.caretModel.moveToOffset(tailOffset + 2)
    }
  }
}