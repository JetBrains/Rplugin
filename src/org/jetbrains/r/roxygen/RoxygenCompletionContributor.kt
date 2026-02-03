/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.roxygen

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.text.StringUtil
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.editor.completion.GLOBAL_GROUPING
import com.intellij.r.psi.editor.completion.RLookupElement
import com.intellij.r.psi.editor.completion.RLookupElementFactory
import com.intellij.r.psi.editor.completion.RLookupElementInsertHandler
import com.intellij.r.psi.psi.api.RAssignmentStatement
import com.intellij.r.psi.psi.references.RSearchScopeUtil
import com.intellij.r.psi.roxygen.RoxygenLanguage
import com.intellij.r.psi.roxygen.RoxygenUtil
import com.intellij.r.psi.roxygen.psi.RoxygenElementFilters
import com.intellij.r.psi.roxygen.psi.api.RoxygenIdentifierExpression
import com.intellij.r.psi.roxygen.psi.api.RoxygenNamespaceAccessExpression
import com.intellij.util.ProcessingContext
import org.jetbrains.r.editor.completion.RPackageCompletionUtil

class RoxygenCompletionContributor : CompletionContributor() {

  override fun beforeCompletion(context: CompletionInitializationContext) {
    context.dummyIdentifier = CompletionUtil.DUMMY_IDENTIFIER_TRIMMED
  }

  init {
    addTagNamesCompletion()
    addParameterCompletion()
    addNamespaceAccessExpression()
    addIdentifierCompletion()
  }

  private fun addTagNamesCompletion() {
    extend(CompletionType.BASIC, psiElement()
      .withLanguage(RoxygenLanguage.INSTANCE)
      .and(RoxygenElementFilters.TAG_NAME_FILTER), TagNameCompletionProvider())
  }

  private fun addParameterCompletion() {
    extend(CompletionType.BASIC, psiElement()
      .withLanguage(RoxygenLanguage.INSTANCE)
      .andOr(RoxygenElementFilters.PARAMETER_FILTER), ParameterCompletionProvider())
  }

  private fun addNamespaceAccessExpression() {
    extend(CompletionType.BASIC, psiElement()
      .withLanguage(RoxygenLanguage.INSTANCE)
      .and(RoxygenElementFilters.NAMESPACE_ACCESS_FILTER), NamespaceAccessCompletionProvider())
  }

  private fun addIdentifierCompletion() {
    extend(CompletionType.BASIC, psiElement()
      .withLanguage(RoxygenLanguage.INSTANCE)
      .andOr(RoxygenElementFilters.IDENTIFIER_FILTER), IdentifierCompletionProvider())
  }

  private inner class TagNameCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, _result: CompletionResultSet) {
      val result = _result.withPrefixMatcher("@" + _result.prefixMatcher.prefix)
      TAG_NAMES.forEach { result.consumeTag(it) }
    }

    private fun CompletionResultSet.consumeTag(tagName: String) {
      consume(PrioritizedLookupElement.withGrouping(RLookupElement ("@$tagName", true), GLOBAL_GROUPING))
    }
  }

  private inner class ParameterCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
      val function = RoxygenUtil.findAssociatedFunction(parameters.position)
      function?.parameterList?.parameterList?.forEach {
        result.addElement(RLookupElementFactory().createLocalVariableLookupElement(it.name, true))
      }
    }
  }

  private class NamespaceAccessCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
      val position =
        PsiTreeUtil.getParentOfType(parameters.position, RoxygenIdentifierExpression::class.java, false) ?: return
      val namespaceAccess = position.parent as? RoxygenNamespaceAccessExpression ?: return
      val namespaceName = namespaceAccess.namespaceName
      RPackageCompletionUtil.addNamespaceCompletion(namespaceName, false, parameters, result, roxygenLinkCompletionElementFactory)
    }
  }

  private inner class IdentifierCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
      val position =
        PsiTreeUtil.getParentOfType(parameters.position, RoxygenIdentifierExpression::class.java, false) ?: return
      RPackageCompletionUtil.addPackageCompletion(position, result)
      val originalFile = parameters.originalFile
      val prefix = position.name.let { StringUtil.trimEnd(it, CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED) }
      RPackageCompletionUtil.addCompletionFromIndices(position.project, RSearchScopeUtil.getScope(originalFile),
                                                      originalFile, prefix, HashSet(), result, roxygenLinkCompletionElementFactory)
    }
  }

  companion object {
    private val roxygenLinkCompletionElementFactory = RLookupElementFactory(RoxygenFunctionLinkInsertHandler,
                                                                            RoxygenConstantLinkInsertHandler)

    private object RoxygenFunctionLinkInsertHandler : RLookupElementInsertHandler {
      override fun getInsertHandlerForAssignment(assignment: RAssignmentStatement): InsertHandler<LookupElement> {
        return getInsertHandlerForFunctionCall("")
      }

      override fun getInsertHandlerForFunctionCall(functionParameters: String) = InsertHandler<LookupElement> { context, _ ->
        val offset = context.tailOffset
        val document = context.document
        insertSpaceAfterLinkIfNeeded(document, offset)
        document.insertString(offset, "()")
        context.editor.caretModel.moveCaretRelatively(4, 0, false, false, false)
      }

      override fun getInsertHandlerForLookupString(lookupString: String) = InsertHandler<LookupElement> { context, _ ->
        val offset = context.tailOffset
        val document = context.document
        insertSpaceAfterLinkIfNeeded(document, offset)
        document.insertString(offset, "()")
        context.editor.caretModel.moveCaretRelatively(4, 0, false, false, false)
      }
    }

    private object RoxygenConstantLinkInsertHandler : RLookupElementInsertHandler {
      override fun getInsertHandlerForAssignment(assignment: RAssignmentStatement) = InsertHandler<LookupElement> { context, _ ->
        val document = context.document
        insertSpaceAfterLinkIfNeeded(document, context.tailOffset)
        context.editor.caretModel.moveCaretRelatively(2, 0, false, false, false)
      }

      override fun getInsertHandlerForLookupString(lookupString: String) = InsertHandler<LookupElement> { context, _ ->
        val document = context.document
        insertSpaceAfterLinkIfNeeded(document, context.tailOffset)
        context.editor.caretModel.moveCaretRelatively(2, 0, false, false, false)
      }
    }

    private fun insertSpaceAfterLinkIfNeeded(document: Document, tailOffset: Int) {
      val text = document.text
      if (tailOffset + 1 >= text.length || text[tailOffset + 1] != ' ') {
        document.insertString(tailOffset + 1, " ")
      }
    }

    private val TAG_NAMES: List<String> =
      listOf("aliases", "author", "backref", "concept", "describeIn", "description", "details", "docType", "encoding", "eval",
             "evalNamespace", "evalRd", "example", "examples", "export", "exportClass", "exportMethod", "exportPattern",
             "exportS3Method", "family", "field", "format", "import", "importClassesFrom", "importFrom", "importMethodsFrom",
             "include", "includeRmd", "inherit", "inheritDotParams", "inheritParams", "inheritSection", "keywords", "md", "method",
             "name", "noMd", "noRd", "note", "order", "param", "rawNamespace", "rawRd", "rdname", "references", "return", "returns",
             "section", "seealso", "slot", "source", "template", "templateVar", "title", "usage", "useDynLib")
  }
}