/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.rename

import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.inplace.VariableInplaceRenamer
import org.jetbrains.r.classes.s4.classInfo.RStringLiteralPomTarget
import org.jetbrains.r.classes.s4.context.RS4ContextProvider
import org.jetbrains.r.classes.s4.context.methods.RS4SetGenericFunctionNameContext
import org.jetbrains.r.classes.s4.context.methods.RS4SetMethodFunctionNameContext
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.psi.RPomTarget
import org.jetbrains.r.psi.RRecursiveElementVisitor
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.isFunctionFromLibrary
import org.jetbrains.r.psi.references.RSearchScopeUtil
import org.jetbrains.r.psi.stubs.classes.RS4GenericIndex
import org.jetbrains.r.psi.stubs.classes.RS4MethodsIndex

class RVariableInplaceRenamer : VariableInplaceRenamer {

  constructor(elementToRename: PsiNamedElement,
              editor: Editor) : super(substituteS4MethodForRename(elementToRename), editor, elementToRename.project)

  constructor(elementToRename: PsiNamedElement?,
              editor: Editor,
              project: Project,
              initialName: String?,
              oldName: String?) : super(substituteS4MethodForRename(elementToRename, project), editor, project, initialName, oldName)

  override fun createInplaceRenamerToRestart(variable: PsiNamedElement, editor: Editor, initialName: String): VariableInplaceRenamer {
    return RVariableInplaceRenamer(variable, editor, myProject, initialName, myOldName)
  }

  override fun getVariable(): PsiNamedElement? {
    if (myRenameOffset != null) {
      val file = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument()) ?: return null
      return PsiTreeUtil.findElementOfClassAtRange(file, myRenameOffset.getStartOffset(),
                                                   myRenameOffset.getEndOffset(),
                                                   RIdentifierExpression::class.java)
    }
    return super.getVariable()
  }

  override fun getNameIdentifier(): PsiElement? {
    val element = myElementToRename
    if (element is RStringLiteralExpression) return element
    if (element is PomTargetPsiElement) {
      (element.target as? RStringLiteralPomTarget)?.let { return it.literal }
    }
    return super.getNameIdentifier()
  }

  override fun addAdditionalVariables(builder: TemplateBuilderImpl) {
    val element = myElementToRename as? PomTargetPsiElement ?: return
    val target = element.target as? RStringLiteralPomTarget ?: return
    val literal = target.literal
    val context = RS4ContextProvider.getS4Context(literal, RS4SetGenericFunctionNameContext::class) ?: return
    val name = literal.name ?: return
    RS4MethodsIndex.findDefinitionsByName(name, element.project, RSearchScopeUtil.getScope(element)).forEach {
      if (it is RCallExpression) {
        val methodLiteral = RArgumentInfo.getArgumentByName(it, "f") as? RStringLiteralExpression ?: return@forEach
        builder.replaceElement(methodLiteral)
      }
    }
    val body = when(val def = RArgumentInfo.getArgumentByName(context.functionCall, "def")) {
      is RFunctionExpression -> def.expression
      is RIdentifierExpression -> ((def.reference.resolve() as? RAssignmentStatement)?.assignedValue as? RFunctionExpression)?.expression
      else -> null
    }
    body?.accept(object : RRecursiveElementVisitor() {
      override fun visitCallExpression(o: RCallExpression) {
        if (o.isFunctionFromLibrary("standardGeneric", "base")) {
          (RArgumentInfo.getArgumentByName(o, "f") as? RStringLiteralExpression)?.let { builder.replaceElement(it) }
        }
      }
    })
  }

  private fun TemplateBuilderImpl.replaceElement(str: RStringLiteralExpression) =
    replaceElement(str, getRangeToRename(str), OTHER_VARIABLE_NAME, PRIMARY_VARIABLE_NAME, false)

  override fun acceptReference(reference: PsiReference): Boolean {
    return RenameUtil.acceptReference(reference, myElementToRename)
  }

  override fun getRangeToRename(reference: PsiReference): TextRange {
    return RenameUtil.fixTextRange(super.getRangeToRename(reference), reference)
  }

  override fun getRangeToRename(element: PsiElement): TextRange {
    if (element is RStringLiteralExpression) {
      return ElementManipulators.getValueTextRange(element)
    }
    return RenameUtil.fixTextRange(super.getRangeToRename(element), element)
  }

  override fun checkLocalScope(): PsiElement? {
    val currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument())
    return currentFile ?: super.checkLocalScope()
  }

  companion object {
    private fun substituteS4MethodForRename(elementToRename: PsiNamedElement?, project: Project? = null): PsiNamedElement? {
      val literal = when (elementToRename) {
        is RStringLiteralExpression -> elementToRename
        is PomTargetPsiElement -> (elementToRename.target as? RStringLiteralPomTarget)?.literal
        else -> null
      } ?: return elementToRename
      RS4ContextProvider.getS4Context(literal, RS4SetMethodFunctionNameContext::class) ?: return elementToRename
      val name = literal.name ?: return elementToRename
      val generic = RS4GenericIndex.findDefinitionsByName(
        name, project ?: literal.project,
        RSearchScopeUtil.getScope(literal)
      ).singleOrNull() as? RCallExpression ?: return elementToRename
      return (RArgumentInfo.getArgumentByName(generic, "name") as? RStringLiteralExpression)?.let {
        RPomTarget.createStringLiteralTarget(it) as PsiNamedElement
      }
    }
  }
}