/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.roxygen

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.r.editor.GLOBAL_GROUPING
import org.jetbrains.r.editor.RLookupElement
import org.jetbrains.r.editor.VARIABLE_GROUPING
import org.jetbrains.r.roxygen.psi.RoxygenElementFilters

class RoxygenCompletionContributor : CompletionContributor() {

  override fun beforeCompletion(context: CompletionInitializationContext) {
    context.dummyIdentifier = CompletionUtil.DUMMY_IDENTIFIER_TRIMMED
  }

  init {
    addTagNamesCompletion()
    addParameterCompletion()
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

  private inner class TagNameCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, _result: CompletionResultSet) {
      val result = _result.withPrefixMatcher("@" + _result.prefixMatcher.prefix)
      TAG_NAMES.forEach { result.consumeTag(it) }
    }

    private fun CompletionResultSet.consumeTag(tagName: String) {
      consume(PrioritizedLookupElement.withGrouping(RLookupElement("@$tagName", true), GLOBAL_GROUPING))
    }
  }

  private inner class ParameterCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
      val function = RoxygenUtil.findAssociatedFunction(parameters.position)
      function?.parameterList?.parameterList?.forEach { result.addElement(createParameterLookupElement(it.name)) }
    }
  }

  companion object {
    private fun createParameterLookupElement(lookupString: String): LookupElement {
      return PrioritizedLookupElement.withGrouping(RLookupElement(lookupString, true, AllIcons.Nodes.Parameter), VARIABLE_GROUPING)
    }

    private val TAG_NAMES =
      listOf<String>("aliases", "author", "backref", "concept", "describeIn", "description", "details", "docType", "encoding", "eval",
                     "evalNamespace", "evalRd", "example", "examples", "export", "exportClass", "exportMethod", "exportPattern",
                     "exportS3Method", "family", "field", "format", "import", "importClassesFrom", "importFrom", "importMethodsFrom",
                     "include", "includeRmd", "inherit", "inheritDotParams", "inheritParams", "inheritSection", "keywords", "md", "method",
                     "name", "noMd", "noRd", "note", "order", "param", "rawNamespace", "rawRd", "rdname", "references", "return", "returns",
                     "section", "seealso", "slot", "source", "template", "templateVar", "title", "usage", "useDynLib")
  }
}