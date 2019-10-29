// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2011-present Greg Shrago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.r.refactoring

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.ui.UsageViewDescriptorAdapter
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.util.IncorrectOperationException
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RIdentifierExpression
import org.jetbrains.r.psi.api.RPsiElement


/**
 * Inlining of assignment statements.
 */
class InlineAssignmentProcessor(private var expression: RAssignmentStatement,
                                project: Project,
                                private val reference: PsiReference?,
                                private val inlineThisOnly: Boolean) : BaseRefactoringProcessor(project) {

  override fun createUsageViewDescriptor(usages: Array<UsageInfo>): UsageViewDescriptor {
    return object : UsageViewDescriptorAdapter() {
      override fun getElements(): Array<PsiElement> {
        return arrayOf<PsiElement>(expression)
      }

      override fun getProcessedElementsHeader(): String {
        return "Expression"
      }
    }
    //        return new BnfInlineViewDescriptor(myExpression);
  }

  override fun getCommandName(): String {
    return "Inline rule '" + expression.name + "'"
  }

  override fun findUsages(): Array<UsageInfo> {
    val assignee = expression.assignee ?: return UsageInfo.EMPTY_ARRAY
    if (inlineThisOnly) return arrayOf(UsageInfo(reference!!.element))

    val result = mutableListOf<UsageInfo>()
    for (reference in ReferencesSearch.search(assignee, expression.useScope, false)) {
      val element = reference.element
      result.add(UsageInfo(element))
    }
    return result.toTypedArray()
  }

  override fun refreshElements(elements: Array<PsiElement>) {
    LOG.assertTrue(elements.size == 1 && elements[0] is RAssignmentStatement)
    expression = elements[0] as RAssignmentStatement
  }

  override fun performRefactoring(usages: Array<UsageInfo>) {
    CommonRefactoringUtil.sortDepthFirstRightLeftOrder(usages)
    if (expression.assignedValue == null) {
      return
    }

    for (info in usages) {
      try {
        if (info.element !is RIdentifierExpression) {
          continue
        }
        val element = info.element as RIdentifierExpression
        inlineExpressionUsage(element, expression.assignedValue!!)
      }
      catch (e: IncorrectOperationException) {
        LOG.error(e)
      }
    }

    if (!inlineThisOnly) {
      try {
        expression.delete()
      }
      catch (e: IncorrectOperationException) {
        LOG.error(e)
      }
    }
  }

  companion object {
    private val LOG = Logger.getInstance(InlineAssignmentProcessor::class.java)

    private fun inlineExpressionUsage(place: PsiElement, ruleExpr: RPsiElement) {
      val replacement = RElementFactory.createLeafFromText(ruleExpr.getProject(), ruleExpr.getText());
      place.replace(replacement)
    }
  }
}
