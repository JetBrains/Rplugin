// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.util.Consumer
import com.intellij.util.ProcessingContext
import com.intellij.util.Processor
import icons.org.jetbrains.r.psi.TableManipulationColumn
import icons.org.jetbrains.r.psi.TableManipulationContextType
import icons.org.jetbrains.r.psi.TableType
import org.apache.commons.lang.StringUtils
import org.jetbrains.r.RLanguage
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.editor.completion.AES_PARAMETER_FILTER
import org.jetbrains.r.editor.completion.GGPlot2AesColumnCompletionProvider
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.packages.RPackage
import org.jetbrains.r.parsing.RElementTypes.*
import org.jetbrains.r.psi.*
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.references.RSearchScopeUtil
import org.jetbrains.r.psi.stubs.RAssignmentCompletionIndex
import org.jetbrains.r.psi.stubs.RInternalAssignmentCompletionIndex
import org.jetbrains.r.refactoring.RNamesValidator
import org.jetbrains.r.rinterop.RValueFunction
import javax.swing.Icon

const val TABLE_MANIPULATION_COLUMNS_GROUPING = 110
const val NAMED_ARGUMENT_GROUPING = 100
const val VARIABLE_GROUPING = 90
const val PACKAGE_GROUPING = -1
const val GLOBAL_GROUPING = 0
const val NAMESPACE_NAME_GROUPING = -1

class RLookupElement(private val lookup: String,
                     private val bold: Boolean,
                     private val icon: Icon? = null,
                     private val packageName: String? = null,
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

class RCompletionContributor : CompletionContributor() {

  init {
    addInstalledPackageCompletion()
    addIdentifierCompletion()
    addMemberAccessCompletion()
    addNamespaceAccessExpression()
    addDplyrContextCompletion()
    addDataTableContextCompletion()
    addStringLiteralCompletion()
    addGGPlotAesColumnCompletionProvider()
  }

  private fun addNamespaceAccessExpression() {
    extend(CompletionType.BASIC, psiElement()
      .withLanguage(RLanguage.INSTANCE)
      .and(RElementFilters.NAMESPACE_REFERENCE_FILTER), NamespaceAccessCompletionProvider())
  }

  private fun addIdentifierCompletion() {
    extend(CompletionType.BASIC, psiElement()
      .withLanguage(RLanguage.INSTANCE)
      .and(RElementFilters.IDENTIFIER_FILTER), IdentifierCompletionProvider())
  }

  private fun addMemberAccessCompletion() {
    extend(CompletionType.BASIC, psiElement().withLanguage(RLanguage.INSTANCE)
      .and(RElementFilters.MEMBER_ACCESS_FILTER), MemberAccessCompletionProvider())
  }

  private fun addInstalledPackageCompletion() {
    extend(CompletionType.BASIC, psiElement().withLanguage(RLanguage.INSTANCE)
      .and(RElementFilters.IMPORT_CONTEXT), InstalledPackageCompletionProvider())
  }

  private fun addDplyrContextCompletion() {
    extend(CompletionType.BASIC, psiElement().withLanguage(RLanguage.INSTANCE)
      .and(RElementFilters.IDENTIFIER_OR_STRING_FILTER), DplyrContextCompletionProvider())
  }

  private fun addDataTableContextCompletion() {
    extend(CompletionType.BASIC, psiElement().withLanguage(RLanguage.INSTANCE)
      .and(RElementFilters.IDENTIFIER_OR_STRING_FILTER), DataTableContextCompletionProvider())
  }

  private fun addStringLiteralCompletion() {
    extend(CompletionType.BASIC, psiElement().withLanguage(RLanguage.INSTANCE)
      .and(RElementFilters.STRING_FILTER), StringLiteralCompletionProvider())
  }

  private fun addGGPlotAesColumnCompletionProvider() {
    extend(CompletionType.BASIC, psiElement().withLanguage(RLanguage.INSTANCE)
      .and(AES_PARAMETER_FILTER), GGPlot2AesColumnCompletionProvider())
  }

  private class MemberAccessCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
      val position = parameters.position
      val file = parameters.originalFile
      val info = file.runtimeInfo ?: return
      val memberAccess = PsiTreeUtil.getParentOfType(position, org.jetbrains.r.psi.api.RMemberExpression::class.java) ?: return
      val leftExpr = memberAccess.leftExpr ?: return
      val noCalls = PsiTreeUtil.processElements(leftExpr) { it !is RCallExpression }
      if (noCalls) {
        info.loadObjectNames(leftExpr.text).forEach { result.consume(createNamespaceAccess(it)) }
      }
    }
  }


  private class InstalledPackageCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
      val position = parameters.position
      val interpreter = RInterpreterManager.getInterpreter(position.project) ?: return
      val installedPackages = interpreter.installedPackages
      installedPackages.filter { it.isUser }.forEach {
        result.consume(createPackageLookupElement(it.packageName, true))
      }
    }
  }

  private class NamespaceAccessCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
      val position = PsiTreeUtil.getParentOfType(parameters.position, RIdentifierExpression::class.java, false)
                     ?: return
      val project = position.project
      val namespaceAccess = position.parent as? RNamespaceAccessExpression ?: return
      val namespaceName = namespaceAccess.namespaceName
      val interpreter = RInterpreterManager.getInterpreter(project) ?: return
      val packageFile = interpreter.getSkeletonFileByPackageName(namespaceName) ?: return
      val scope = GlobalSearchScope.fileScope(packageFile)
      val isInternalAccess = namespaceAccess.node.findChildByType(R_TRIPLECOLON) != null
      addCompletionFromIndices(project, scope, parameters.originalFile, "", HashSet(), result,
                               isInternalAccess = isInternalAccess)
    }
  }

  private class IdentifierCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
      val position = PsiTreeUtil.getParentOfType(parameters.position, RExpression::class.java, false) ?: return
      val parent = position.parent
      val shownNames = HashSet<String>()
      val project = position.project
      val originalFile = parameters.originalFile

      // don't complete parameters name
      if (parent is RParameter && position == parent.variable) {
        return
      }

      val file = parameters.originalFile
      val isHelpFromRConsole = file.getUserData(RConsoleView.IS_R_CONSOLE_KEY)?.let { file.firstChild is RHelpExpression } ?: false
      addKeywords(position, shownNames, result, isHelpFromRConsole)
      addLocalsFromControlFlow(position, shownNames, result, isHelpFromRConsole)
      addLocalsFromRuntime(originalFile, shownNames, result)

      // we are completing an assignee, so we don't want to suggest function names here
      if (position.isAssignee()) {
        return
      }

      addPackageCompletion(position, result)
      addNamedArgumentsCompletion(originalFile, parent, result)
      val prefix = position.name?.let { StringUtil.trimEnd(it, CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED) } ?: ""
      addCompletionFromIndices(project, RSearchScopeUtil.getScope(originalFile), parameters.originalFile, prefix, shownNames, result,
                               isHelpFromRConsole)
    }

    private fun addLocalsFromRuntime(originFile: PsiFile, shownNames: HashSet<String>, result: CompletionResultSet) {
      originFile.runtimeInfo?.variables?.let { variables ->
        variables.filterKeys { shownNames.add(it) }.forEach { (name, value) ->
          if (value is RValueFunction) {
            val code = "${name} <- ${value.header} NULL"
            val element = RElementFactory.createRPsiElementFromText(originFile.project, code) as? RAssignmentStatement ?: return@forEach
            result.consume(createFunctionLookupElement(element, isHelpFromRConsole = false, isLocal = true))
          }
          else {
            result.consume(createLocalVariableLookupElement(name, false))
          }
        }
      }
    }

    private fun addKeywords(position: RExpression,
                            shownNames: HashSet<String>,
                            result: CompletionResultSet,
                            isHelpFromRConsole: Boolean) {
      for (keyword in BUILTIN_CONSTANTS + KEYWORDS) {
        val necessaryCondition = KEYWORD_NECESSARY_CONDITION[keyword]
        val stringValue = keyword.toString()
        if (isHelpFromRConsole || necessaryCondition == null || necessaryCondition(position)) {
          shownNames.add(stringValue)
          result.addElement(PrioritizedLookupElement.withGrouping(RLookupElement(stringValue, true), GLOBAL_GROUPING))
        }
      }
      for (keyword in KEYWORDS_WITH_BRACKETS) {
        val stringValue = keyword.toString()
        shownNames.add(stringValue)
        result.addElement(PrioritizedLookupElement.withInsertHandler(
          PrioritizedLookupElement.withGrouping(RLookupElement(stringValue, true, tailText = " (...)"), GLOBAL_GROUPING),
          InsertHandler<LookupElement> { context, _ ->
            if (!isHelpFromRConsole) {
              val document = context.document
              document.insertString(context.tailOffset, " ()")
              context.editor.caretModel.moveCaretRelatively(2, 0, false, false, false)
            }
          }))
      }
    }

    private fun addLocalsFromControlFlow(position: RExpression,
                                         shownNames: HashSet<String>,
                                         result: CompletionResultSet,
                                         isHelpFromRConsole: Boolean) {
      val controlFlowHolder = PsiTreeUtil.getParentOfType(position, RControlFlowHolder::class.java)
      controlFlowHolder?.getLocalVariableInfo(position)?.variables?.values?.sortedBy { it.variableDescription.name }?.forEach {
        val name = it.variableDescription.name
        shownNames.add(name)
        val parent = it.variableDescription.firstDefinition.parent
        if (parent is RAssignmentStatement && parent.isFunctionDeclaration) {
          result.consume(createFunctionLookupElement(parent, isHelpFromRConsole, true))
        }
        else {
          result.consume(createLocalVariableLookupElement(name, parent is RParameter))
        }
      }
    }

    private fun consumeParameter(parameterName: String, shownNames: MutableSet<String>, result: CompletionResultSet) {
      if (shownNames.add(parameterName)) {
        result.consume(createNamedArgumentLookupElement(parameterName))
      }
    }

    private fun addNamedArgumentsCompletion(originalFile: PsiFile, parent: PsiElement?, result: CompletionResultSet) {
      if (parent !is RArgumentList) return

      val mainCall = parent.parent as? RCallExpression ?: return
      val shownNames = HashSet<String>()

      for (functionDeclaration in RPsiUtil.resolveCall(mainCall)) {
        functionDeclaration.parameterNameList.forEach { consumeParameter(it, shownNames, result) }
      }

      val info = originalFile.runtimeInfo ?: return
      val mainFunctionName = when (val expression = mainCall.expression) {
        is RNamespaceAccessExpression -> expression.identifier?.name ?: return
        is RIdentifierExpression -> expression.name
        else -> return
      }
      val namedArguments = info.loadAllNamedArguments("'$mainFunctionName'")

      for (parameter in namedArguments) {
        consumeParameter(parameter, shownNames, result)
      }
    }

    companion object {
      private val BUILTIN_CONSTANTS = listOf(R_TRUE, R_FALSE, R_NULL, R_NA, R_INF, R_NAN,
                                             R_NA_INTEGER_, R_NA_REAL_, R_NA_COMPLEX_, R_NA_CHARACTER_)
      private val KEYWORDS_WITH_BRACKETS = listOf(R_IF, R_WHILE, R_FUNCTION, R_FOR)
      private val KEYWORDS = listOf(R_ELSE, R_REPEAT, R_IN)
      private val KEYWORD_NECESSARY_CONDITION = mapOf<IElementType, (PsiElement) -> Boolean>(
        R_IN to { element ->
          if (element.parent !is RForStatement) false
          else {
            val newText = element.parent.text.replace(element.text, R_IN.toString())
            val newElement = RElementFactory
              .buildRFileFromText(element.project, newText).findElementAt(element.textOffset - element.parent.textOffset)
            if (PsiTreeUtil.getParentOfType(newElement, PsiErrorElement::class.java, false) != null) false
            else {
              !isErrorElementBefore(newElement!!)
            }
          }
        },
        R_ELSE to { element ->
          var sibling: PsiElement? = PsiTreeUtil.skipWhitespacesAndCommentsBackward(element)
          while (sibling?.elementType == R_NL) {
            sibling = PsiTreeUtil.skipWhitespacesAndCommentsBackward(sibling)
          }
          sibling is RIfStatement && !sibling.node.getChildren(null).any { it.elementType == R_ELSE }
        }
      )

      private val PsiElement.prevLeafs: Sequence<PsiElement>
        get() = generateSequence({ PsiTreeUtil.prevLeaf(this) }, { PsiTreeUtil.prevLeaf(it) })

      private fun isErrorElementBefore(token: PsiElement): Boolean {
        for (leaf in token.prevLeafs) {
          if (leaf is PsiWhiteSpace || leaf is PsiComment) continue
          if (leaf is PsiErrorElement || PsiTreeUtil.findFirstParent(leaf) { it is PsiErrorElement } != null) return true
          if (leaf.textLength != 0) break
        }
        return false
      }
    }
  }

  private class DplyrContextCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
      val position = parameters.position
      val (dplyrCallInfo, currentArgument) = RDplyrUtil.getContextInfo(position) ?: return
      val runtimeInfo = parameters.originalFile.runtimeInfo ?: return
      val tableInfo = RDplyrUtil.getTableColumns(dplyrCallInfo.arguments.firstOrNull() ?: return, runtimeInfo)
      var columns = if (dplyrCallInfo.function.contextType != TableManipulationContextType.SUBSCRIPTION &&
                        dplyrCallInfo.function.tableArguments > 0) {
        tableInfo.columns
      }
      else {
        emptyList()
      }
      val expression = PsiTreeUtil.getParentOfType(position, RExpression::class.java, false)
      when (dplyrCallInfo.function.contextType) {
        TableManipulationContextType.NORMAL -> {
          if (expression is RStringLiteralExpression) return
          columns = RDplyrUtil.addCurrentColumns(columns, dplyrCallInfo, currentArgument.index)
        }
        TableManipulationContextType.SUBSCRIPTION -> {
          if (expression is RIdentifierExpression) {
            columns = columns.map { TableManipulationColumn("\"${it.name}\"", it.type) }
          }
        }
        TableManipulationContextType.JOIN -> {
          val currentArg = dplyrCallInfo.arguments[currentArgument.index]
          if (currentArg !is RNamedArgument || currentArg.name != "by") return
          val firstColumns = columns.map { it.name }.toSet()
          val columns2 = RDplyrUtil.getTableColumns(dplyrCallInfo.arguments.getOrNull(1) ?: return, runtimeInfo).columns
            .filter { it.name !in firstColumns }
          columns = columns + columns2
          if (expression is RIdentifierExpression) {
            columns = columns.map { TableManipulationColumn("\"${it.name}\"", it.type) }
          }
        }
        null -> return
      }

      result.addAllElements(columns.map {
        PrioritizedLookupElement.withGrouping(RLookupElement(it.name, true, AllIcons.Nodes.Field, packageName = it.type),
                                              TABLE_MANIPULATION_COLUMNS_GROUPING)
      })
    }
  }

  private class DataTableContextCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
      val position = parameters.position
      val (dataTableCallInfo, currentArgument) = RDataTableUtil.getContextInfo(position) ?: return
      val runtimeInfo = parameters.originalFile.runtimeInfo ?: return
      val tableArguments = dataTableCallInfo.function.tablesArguments
      val isDataTable: Boolean
      var columns = if (tableArguments.isNotEmpty()) {
        val tableInfos = dataTableCallInfo.function.getTableArguments(dataTableCallInfo.psiCall, dataTableCallInfo.arguments, runtimeInfo).map {
          RDataTableUtil.getTableColumns(it, runtimeInfo)
        }
        isDataTable = tableInfos.all { it.type == TableType.DATA_TABLE }
        tableInfos.map { it.columns }.flatten()
          .groupBy { it.name }
          .map { (name, list) ->
            TableManipulationColumn(name, StringUtils.join(list.mapNotNull { it.type }, "/"))
          }
      }
      else {
        // If signature like my_fun <- function(...) {}
        val tableInfo = RDataTableUtil.getTableColumns(dataTableCallInfo.arguments.firstOrNull() ?: return, runtimeInfo)
        isDataTable = tableInfo.type == TableType.DATA_TABLE
        tableInfo.columns
      }

      val isQuotesNeeded = position.parent !is RStringLiteralExpression &&
                           (dataTableCallInfo.function.isQuotesNeeded(dataTableCallInfo.psiCall,
                                                                      dataTableCallInfo.arguments,
                                                                      currentArgument, runtimeInfo) ||
                           (!isDataTable && dataTableCallInfo.function.contextType == TableManipulationContextType.SUBSCRIPTION))

      if (isQuotesNeeded) {
        columns = columns.map { TableManipulationColumn("\"${it.name}\"", it.type) }
      }

      result.addAllElements(columns.map {
        PrioritizedLookupElement.withGrouping(RLookupElement(it.name, true, AllIcons.Nodes.Field, packageName = it.type),
                                              TABLE_MANIPULATION_COLUMNS_GROUPING)
      })
    }
  }

  private class StringLiteralCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
      val stringLiteral = PsiTreeUtil.getParentOfType(parameters.position, RStringLiteralExpression::class.java, false) ?: return
      val parent = stringLiteral.parent as? ROperatorExpression ?: return
      if (!parent.isBinary || (parent.operator?.name != "==" && parent.operator?.name != "!=")) return
      val other = (if (parent.leftExpr == stringLiteral) parent.rightExpr else parent.leftExpr) ?: return
      val runtimeInfo = parameters.originalFile.runtimeInfo ?: return

      val dplyrContextInfo = RDplyrUtil.getContextInfo(stringLiteral)
      val text = if (dplyrContextInfo != null) {
        val dplyrCallInfo = dplyrContextInfo.callInfo
        val name = other.name
        if (dplyrCallInfo.function.contextType != TableManipulationContextType.NORMAL) return
        val table = dplyrCallInfo.arguments[0]
        val command = StringBuilder()
        command.append("(")
        RDplyrUtil.transformExpression(table, command, runtimeInfo, true)
        command.append(")$$name")
        command.toString()
      }
      else {
        if (!RDplyrUtil.isSafe(other, runtimeInfo)) return
        "(${other.text})"
      }
      val values = runtimeInfo.loadDistinctStrings(text).filter { it.isNotEmpty() }
      result.addAllElements(values.map {
        PrioritizedLookupElement.withInsertHandler(
          RLookupElement(escape(it), true, AllIcons.Nodes.Field, itemText = it),
          InsertHandler<LookupElement> { context, _ ->
            context.file.findElementAt(context.editor.caretModel.offset)?.let { element ->
              context.editor.caretModel.moveToOffset(element.textRange.endOffset)
            }
          }
        )
      })
    }

    private val escape = StringUtil.escaper(true, "\"")::`fun`
  }

  companion object {
    private fun addPackageCompletion(position: PsiElement, result: CompletionResultSet) {
      val interpreter = RInterpreterManager.getInterpreter(position.project) ?: return
      val installedPackages = interpreter.installedPackages
      installedPackages.forEach { result.consume(createPackageLookupElement(it.packageName, false)) }
    }

    private fun createGlobalLookupElement(assignment: RAssignmentStatement, isHelpFromRConsole: Boolean): LookupElement {
      return if (assignment.isFunctionDeclaration) {
        createFunctionLookupElement(assignment, isHelpFromRConsole)
      }
      else {
        val name = assignment.name
        val packageName = RPackage.getOrCreateRPackageBySkeletonFile(assignment.containingFile)?.name
        val icon = AllIcons.Nodes.Constant
        PrioritizedLookupElement.withGrouping(RLookupElement(name, false, icon, packageName), GLOBAL_GROUPING)
      }
    }

    private fun createFunctionLookupElement(functionAssignment: RAssignmentStatement,
                                            isHelpFromRConsole: Boolean,
                                            isLocal: Boolean = false): LookupElement {
      val packageName = if (isLocal) null else RPackage.getOrCreateRPackageBySkeletonFile(functionAssignment.containingFile)?.name
      val icon = AllIcons.Nodes.Function
      val tailText = functionAssignment.functionParameters
      val noArgs = tailText == "()"
      return PrioritizedLookupElement.withInsertHandler(
        PrioritizedLookupElement.withGrouping(RLookupElement(functionAssignment.name, false, icon, packageName, tailText),
                                              if (isLocal) VARIABLE_GROUPING else GLOBAL_GROUPING),
        InsertHandler<LookupElement> { context, _ ->
          if (!isHelpFromRConsole) {
            val document = context.document
            document.insertString(context.tailOffset, "()")
            context.editor.caretModel.moveCaretRelatively(if (noArgs) 2 else 1, 0, false, false, false)
          }
        }
      )
    }

    private fun createLocalVariableLookupElement(lookupString: String, isParameter: Boolean): LookupElement {
      val icon = if (isParameter) AllIcons.Nodes.Parameter else AllIcons.Nodes.Variable
      return PrioritizedLookupElement.withGrouping(RLookupElement(lookupString, true, icon), VARIABLE_GROUPING)
    }

    private fun createNamedArgumentLookupElement(lookupString: String): LookupElement {
      val icon = AllIcons.Nodes.Parameter
      return PrioritizedLookupElement.withInsertHandler(
        PrioritizedLookupElement.withGrouping(RLookupElement(lookupString, true, icon, tailText = " = "), NAMED_ARGUMENT_GROUPING),
        InsertHandler<LookupElement> { context, _ ->
          val document = context.document
          document.insertString(context.tailOffset, " = ")
          context.editor.caretModel.moveCaretRelatively(3, 0, false, false, false)
        }
      )
    }

    private fun createPackageLookupElement(lookupString: String, inImport: Boolean): LookupElement {
      return if (inImport) {
        PrioritizedLookupElement.withGrouping(RLookupElement(lookupString, true, AllIcons.Nodes.Package), PACKAGE_GROUPING)
      }
      else {
        PrioritizedLookupElement.withInsertHandler(
          PrioritizedLookupElement.withGrouping(RLookupElement(lookupString, true, AllIcons.Nodes.Package,
                                                               tailText = "::"), PACKAGE_GROUPING),
          InsertHandler<LookupElement> { context, _ ->
            val document = context.document
            document.insertString(context.tailOffset, "::")
            AutoPopupController.getInstance(context.project).scheduleAutoPopup(context.editor)
            context.editor.caretModel.moveCaretRelatively(2, 0, false, false, false)
          })
      }
    }

    private fun createNamespaceAccess(lookupString: String): LookupElement {
      return PrioritizedLookupElement.withInsertHandler(
        PrioritizedLookupElement.withGrouping(RLookupElement(lookupString, false, AllIcons.Nodes.Package), NAMESPACE_NAME_GROUPING),
        InsertHandler<LookupElement> { context, _ ->
          val document = context.document
          document.replaceString(context.startOffset, context.tailOffset, RNamesValidator.quoteIfNeeded(lookupString))
          context.editor.caretModel.moveToOffset(context.tailOffset)
        })
    }


    private fun addCompletionFromIndices(project: Project,
                                         scope: GlobalSearchScope,
                                         originFile: PsiFile,
                                         prefix: String,
                                         shownNames: HashSet<String>,
                                         result: CompletionResultSet,
                                         isHelpFromRConsole: Boolean = false, isInternalAccess: Boolean = false) {
      val runtimeScope = computeRuntimeScope(originFile)
      var hasElementsWithPrefix = false

      if (runtimeScope != null) {
        processElementsFromIndex(project, runtimeScope.intersectWith(scope), shownNames, Consumer<LookupElement> {
          if (it.lookupString.startsWith(prefix)) {
            hasElementsWithPrefix = true
          }
          result.consume(it)
        }, isHelpFromRConsole, isInternalAccess)
      }
      if (!hasElementsWithPrefix) {
        processElementsFromIndex(project, scope, shownNames, result, isHelpFromRConsole, isInternalAccess)
      }
    }

    private fun processElementsFromIndex(project: Project,
                                         scope: GlobalSearchScope,
                                         shownNames: HashSet<String>,
                                         result: Consumer<LookupElement>,
                                         isHelpFromRConsole: Boolean,
                                         isInternalAccess: Boolean) {

      val indexAccessor = if (isInternalAccess) RInternalAssignmentCompletionIndex else RAssignmentCompletionIndex

      indexAccessor.process("", project, scope, Processor { assignment ->
        if (!shownNames.add(assignment.name)) {
          return@Processor true
        }
        result.consume(createGlobalLookupElement(assignment, isHelpFromRConsole))
        return@Processor true
      })
    }

    private fun computeRuntimeScope(originFile: PsiFile): GlobalSearchScope? {
      return originFile.runtimeInfo?.let { runtimeInfo ->
        val interpreter = RInterpreterManager.getInterpreter(originFile.project) ?: return null
        val loadedPackages = runtimeInfo.loadedPackages.keys.mapNotNull { interpreter.getSkeletonFileByPackageName(it)?.virtualFile }
        GlobalSearchScope.filesScope(originFile.project, loadedPackages)
      }
    }
  }
}
