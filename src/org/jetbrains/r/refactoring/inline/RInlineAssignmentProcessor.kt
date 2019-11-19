/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.inline

import com.intellij.history.LocalHistory
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.util.IncorrectOperationException
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.getParameters

/**
 * Inlining of variables and functions.
 */
class RInlineAssignmentProcessor(private val project: Project,
                                 private val editor: Editor?,
                                 private val assignment: RAssignmentStatement,
                                 private val refElement: RIdentifierExpression?,
                                 private val inlineThisOnly: Boolean,
                                 private val isFunction: Boolean,
                                 removeDefinition: Boolean) : BaseRefactoringProcessor(project) {

  private val removeDefinition = !inlineThisOnly && (!isFunction || isFunction && removeDefinition)

  override fun createUsageViewDescriptor(usages: Array<UsageInfo>) = object : UsageViewDescriptor {
    override fun getElements(): Array<PsiElement> = arrayOf(assignment)
    override fun getProcessedElementsHeader(): String = RBundle.message("inline.local.processor.usage.view.header")
    override fun getCodeReferencesText(usagesCount: Int, filesCount: Int): String = RBundle.message(
      "inline.local.processor.usage.view.ref.text")

    override fun getCommentReferencesText(usagesCount: Int, filesCount: Int): String = ""
  }

  override fun getCommandName(): String = RBundle.message("inline.local.processor.command.name", assignment.name)

  override fun findUsages(): Array<UsageInfo> {
    val name = assignment.assignee?.name ?: return UsageInfo.EMPTY_ARRAY
    if (inlineThisOnly) return arrayOf(UsageInfo(refElement!!))
    val controlFlow = RInlineUtil.getScope(assignment).controlFlow
    return RInlineUtil.getPostRefs(controlFlow, name, assignment).map { UsageInfo(it) }.toTypedArray()
  }

  override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
    try {
      if (refUsages.isNull) return false
      val usages = refUsages.get()
      CommonRefactoringUtil.sortDepthFirstRightLeftOrder(usages)
      for (info in usages) {
        val element = info.element as? RIdentifierExpression ?: continue
        val controlFlow = RInlineUtil.getScope(element).controlFlow
        if (RInlineUtil.getLatestDefs(controlFlow, element.name, element).any { it != assignment }) {
          RInlineUtil.showErrorAndExit(project, editor, RBundle.message("inline.local.processor.error.another.definition")) { return false }
        }
      }
    }
    finally {
      prepareSuccessful()
    }
    return true
  }

  override fun performRefactoring(usages: Array<UsageInfo>) {
    val action = LocalHistory.getInstance().startAction(commandName)
    try {
      CommonRefactoringUtil.sortDepthFirstRightLeftOrder(usages)
      val value = assignment.assignedValue ?: return

      val defaultValues = mutableMapOf<String, PsiElement>()
      val argsNames = mutableListOf<String>()
      if (isFunction) {
        for (param in assignment.getParameters()) {
          val name = param.name
          argsNames.add(param.name)
          val defaultValue = param.defaultValue ?: continue
          defaultValues[name] = defaultValue
        }
      }

      for (info in usages) {
        try {
          val element = info.element as? RIdentifierExpression ?: continue
          if (!isFunction) inlineExpressionUsage(element, value, myProject)
          else inlineFunctionUsage(element, value, myProject, argsNames, defaultValues)
        }
        catch (e: IncorrectOperationException) {
          LOG.error("Cannot inline $info. Cause: $e")
        }
      }

      if (removeDefinition) {
        try {
          assignment.delete()
        }
        catch (e: IncorrectOperationException) {
          LOG.error("Cannot delete ${assignment.text}. Cause: $e")
        }
      }
    }
    finally {
      action.finish()
    }
  }

  companion object {
    private val LOG = Logger.getInstance(RInlineAssignmentProcessor::class.java)

    private fun inlineExpressionUsage(place: PsiElement, ruleExpr: PsiElement, project: Project) {
      val replacement = if (areParenthesesNecessary(ruleExpr, place, place.parent, project)) {
        RElementFactory.createRPsiElementFromText(project, "(${ruleExpr.text})")
      }
      else ruleExpr.copy()
      place.replace(replacement)
    }

    private fun inlineFunctionUsage(place: PsiElement,
                                    functionExpr: PsiElement,
                                    project: Project,
                                    argNames: List<String>,
                                    defaultValues: Map<String, PsiElement>) {
      // Replace as lambda if not call
      if (place.parent !is RCallExpression) {
        inlineExpressionUsage(place, functionExpr, project)
        return
      }

      val call = place.parent as RCallExpression
      val file = call.containingFile
      val scope = RInlineUtil.getScope(call)
      val usedNames = RInlineUtil.collectUsedNames(scope).toMutableSet()
      var firstInScope: PsiElement = call
      while (firstInScope.parent !is RBlockExpression && firstInScope.parent !is RFile) {
        firstInScope = firstInScope.parent
      }

      val functionCopy = functionExpr.copy() as RFunctionExpression
      var dotsArgs = mutableListOf<PsiElement>()
      val realValues = mutableMapOf<String, PsiElement>()
      var cur = 0
      // Named args
      for (arg in call.argumentList.namedArgumentList) {
        realValues[arg.name] = arg.assignedValue ?: error("${arg.text} doesn't have assigned value")
      }

      // Other args
      for (arg in call.argumentList.expressionList) {
        if (arg is RNamedArgument) continue

        while (cur < argNames.size && realValues.containsKey(argNames[cur])) {
          ++cur
        }

        if (cur == argNames.size) continue
        val argName = argNames[cur]
        if (argName == "...") {
          dotsArgs.add(arg)
        } else {
          realValues[argName] = arg
        }
      }
      // Not overlapped default values
      defaultValues.forEach { (k, v) -> realValues.putIfAbsent(k, v) }

      val assignExpressions = mutableListOf<PsiElement>()
      for (arg in argNames) {
        if (arg == "...") continue
        // Ignore if not all argument passed
        val value = realValues[arg] ?: continue
        if (!PsiTreeUtil.instanceOf(value, *SAFE_NO_COPY)) {
          val uniqueName = getUniqueName(arg, usedNames)
          val assign = RElementFactory.createRPsiElementFromText(project, "$uniqueName <- ${value.text}")
          assignExpressions.add(assign)
          val newValue = RElementFactory.createRPsiElementFromText(project, uniqueName)
          realValues[arg] = newValue
        }
      }

      val tmp = mutableListOf<PsiElement>()
      for (dotsArg in dotsArgs) {
        if (!PsiTreeUtil.instanceOf(dotsArg, *SAFE_NO_COPY)) {
          val uniqueName = getUniqueName("dotArg", usedNames)
          val assign = RElementFactory.createRPsiElementFromText(project, "$uniqueName <- ${dotsArg.text}")
          assignExpressions.add(assign)
          val newValue = RElementFactory.createRPsiElementFromText(project, uniqueName)
          tmp.add(newValue)
        }
        else {
          tmp.add(dotsArg)
        }
      }
      dotsArgs = tmp

      val mappingIdentifiers = mutableMapOf<PsiElement, PsiElement>()
      val dotsForReplacing = mutableSetOf<PsiElement>()
      val visitor = object : RInlineUtil.RRecursiveElementVisitor() {
        override fun visitIdentifierExpression(o: RIdentifierExpression) {
          val name = o.name
          var newValue = realValues[name]
          if (newValue == null && name != "...") {
            val parent = o.parent
            if (RPsiUtil.getAssignmentByAssignee(o) != null || parent is RForStatement && parent.target == o) {
              val uniqueName = getUniqueName(name, usedNames)
              newValue = RElementFactory.createRPsiElementFromText(project, uniqueName)
              realValues[name] = newValue
            }
            else {
              usedNames.add(o.name)
              super.visitIdentifierExpression(o)
              return
            }
          }

          if (name == "..." && o.parent is RArgumentList) {
            dotsForReplacing.add(o)
          }
          else if (newValue != null) {
            mappingIdentifiers[o] = newValue
          }
          super.visitIdentifierExpression(o)
        }
      }

      val bodyCopy = functionCopy.expression ?: error("Function without body")
      bodyCopy.accept(visitor)

      mappingIdentifiers.forEach { (k, v) -> k.replace(v) }
      dotsForReplacing.forEach {
        val next = it.nextSibling
        val parent = it.parent
        it.delete()
        val comma = RElementFactory.createLeafFromText(project, ",")
        dotsArgs.forEachIndexed { ind, dotArg ->
          parent.addBefore(dotArg, next)
          if (ind != dotsArgs.size - 1) {
            parent.addBefore(comma, next)
          }
        }
      }

      val returns = RInlineUtil.collectReturns(project, functionCopy)
      var resultName: String? = null
      var singleReturn: RInlineUtil.ReturnResult? = null
      val inserted = mutableListOf<PsiElement>()
      if (returns.size == 1) {
        singleReturn = returns.first()
      }
      else {
        resultName = getUniqueName("result", usedNames)
        returns.forEach { it.doRefactor(resultName) }
      }

      val insertElement = { it: PsiElement -> firstInScope.parent.addBefore(it, firstInScope) }
      assignExpressions.forEach {
        inserted.add(insertElement(it))
      }
      bodyCopy.children.forEach {
        val retStat = singleReturn?.returnStatement
        if (it != retStat || it == retStat && retStat is RForStatement) inserted.add(insertElement(it))
      }

      if (singleReturn != null) {
        val par = call.parent
        if (singleReturn is RInlineUtil.ImplicitNullResult && (par is RFile || par is RBlockExpression && par.expressionList.last() != call)) {
          call.delete()
        }
        else {
          inlineExpressionUsage(call, singleReturn.getPsiReturnValue(), project)
        }
      }
      else {
        inserted.add(call.replace(RElementFactory.createRPsiElementFromText(project, resultName!!)))
      }
      CodeStyleManager.getInstance(project).reformatText(file, inserted.map { it.textRange })
    }

    private fun getUniqueName(oldName: String, usedNames: MutableSet<String>): String {
      if (oldName !in usedNames) {
        usedNames.add(oldName)
        return oldName
      }

      var i = 1
      while ("$oldName$i" in usedNames) {
        ++i
      }
      val name = "$oldName$i"
      usedNames.add(name)
      return name
    }

    private fun areParenthesesNecessary(innerExpression: PsiElement,
                                        currentInner: PsiElement,
                                        parent: PsiElement,
                                        project: Project): Boolean {
      if (PsiTreeUtil.instanceOf(innerExpression, *SAFE_NO_COPY)) {
        return false
      }

      if (PsiTreeUtil.instanceOf(parent, RFile::class.java, RBlockExpression::class.java, RParenthesizedExpression::class.java,
                                 RLoopStatement::class.java, RIfStatement::class.java, RAssignmentStatement::class.java,
                                 RNamedArgument::class.java, RArgumentList::class.java)) {
        return false
      }

      if (parent is RCallExpression && innerExpression is RFunctionExpression) {
        return true
      }

      if (parent is ROperatorExpression) {
        val replaced = parent.copy() as ROperatorExpression
        if (parent.isBinary) {
          if (parent.leftExpr == currentInner) {
            replaced.leftExpr!!.replace(innerExpression)
          }
          else {
            replaced.rightExpr!!.replace(innerExpression)
          }
        }
        else {
          replaced.expr!!.replace(innerExpression)
        }

        val parsed = RElementFactory.createRPsiElementFromText(project, replaced.text) as? ROperatorExpression ?: return true
        if (replaced.isBinary != parsed.isBinary) return true

        return if (replaced.isBinary) {
          replaced.leftExpr!!.text != parsed.leftExpr!!.text
        }
        else {
          replaced.expr!!.text != parsed.expr!!.text
        }
      }

      return true
    }

    private val SAFE_NO_COPY = arrayOf(RNumericLiteralExpression::class.java, RBooleanLiteral::class.java,
                                       RIdentifierExpression::class.java, RNaLiteral::class.java, RNullLiteral::class.java,
                                       RStringLiteralExpression::class.java, RBoundaryLiteral::class.java)
  }
}
