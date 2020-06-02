/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInspection.*
import com.intellij.ide.DataManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.refactoring.RefactoringFactory
import com.intellij.refactoring.rename.RenameHandlerRegistry
import org.jetbrains.r.RBundle
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.refactoring.rename.RNameSuggestionProvider
import org.jetbrains.r.rendering.editor.AdvancedTextEditor

/**
 * newName -> new_name
 *
 * newFunctionName.data.table -> new_function_name.data.table
 */
class NamingConventionInspection : RInspection() {
  override fun getDisplayName() = RBundle.message("inspection.naming.convention.name")

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return Visitor(holder)
  }

  private inner class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitAssignmentStatement(o: RAssignmentStatement) {
      val nameIdentifier = o.assignee as? RIdentifierExpression ?: return
      if (o.isFunctionDeclaration) checkFunctionName(nameIdentifier)
      else checkVariableName(nameIdentifier)
    }

    override fun visitForStatement(o: RForStatement) {
      checkVariableName(o.target ?: return)
    }

    private fun checkVariableName(identifier: RIdentifierExpression) {
      checkName(identifier,
                Regex("([a-z][a-z\\d]*)(_[a-z\\d]+)*"),
                RBundle.message("inspection.naming.convention.variable.description"))
    }

    private fun checkFunctionName(identifier: RIdentifierExpression) {
      if (OPERATORS_REGEX.containsMatchIn(identifier.name)) return
      checkName(identifier,
                Regex("([a-z][a-z\\d]*)(_[a-z\\d]+)*(\\..+)?"),
                RBundle.message("inspection.naming.convention.function.description"))
    }

    private fun checkName(identifier: RIdentifierExpression, regex: Regex, description: String) {
      val targets = identifier.reference.multiResolve(false)
      if (targets.isEmpty() || targets.any { it.element == identifier }) {
        val name = identifier.name
        if (regex.matches(name)) return
        myProblemHolder.registerProblem(identifier, description, ProblemHighlightType.WEAK_WARNING, RenameIdentifierFix)
        return
      }
    }
  }

  private object RenameIdentifierFix : LocalQuickFix {
    override fun getFamilyName() = RBundle.message("inspection.naming.convention.fix.name")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val element = descriptor.psiElement ?: return
      val file = element.containingFile ?: return
      val editorManager = FileEditorManager.getInstance(project)
      val fileEditor = editorManager.getSelectedEditor(file.virtualFile) as? AdvancedTextEditor ?: return renameWithoutEditor(element)
      val dataContext = DataManager.getInstance().getDataContext(fileEditor.textEditor.component)
      val renameHandler = RenameHandlerRegistry.getInstance().getRenameHandler(dataContext)

      val editor = editorManager.selectedTextEditor
      if (editor != null) {
        renameHandler?.invoke(project, editor, file, dataContext)
      }
      else {
        renameHandler?.invoke(project, arrayOf(element), dataContext)
      }
    }

    private fun renameWithoutEditor(element: PsiElement) {
      val factory = RefactoringFactory.getInstance(element.project)
      val names = mutableSetOf<String>()
      val parent = element.parent
      val realElement =
        if (parent is RAssignmentStatement || parent is RParameter) parent
        else element
      RNameSuggestionProvider().getSuggestedNames(realElement, null, names)
      val renameRefactoring = factory.createRename(realElement, names.firstOrNull(), true, true)
      renameRefactoring.run()
    }
  }

  companion object {
    val OPERATORS_REGEX = "^(${listOf("<", "<=", "==", ">", ">=", "\\[", "\\[<-", "\\[\\[", "\\[\\[<-", "\\^",
                                      "\\|", "&", "~", "\\+", "-", "\\*", "/", "%%", "%/%", "!", "\\$").joinToString("|")})".toRegex()
  }
}

