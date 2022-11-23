// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.Nls
import org.jetbrains.r.RBundle
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.intentions.InstallAllFilePackagesFix
import org.jetbrains.r.intentions.InstallPackagesFix
import org.jetbrains.r.interpreter.RInterpreterStateManager
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageInstaller
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.RRecursiveElementVisitor
import org.jetbrains.r.psi.api.*

class MissingPackageInspection : RInspection() {

  @Nls
  override fun getDisplayName(): String {
    return RBundle.message("inspection.missingPackage.name")
  }

  override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
    val problemsHolder = ProblemsHolder(manager, file, isOnTheFly)
    checkFile(file, problemsHolder)
    return problemsHolder.resultsArray
  }

  private fun checkFile(file: PsiFile, problemsHolder: ProblemsHolder) {
    if (file !is RFile) return

    file.accept(object : RRecursiveElementVisitor() {
      override fun visitNamespaceAccessExpression(o: RNamespaceAccessExpression) {
        checkPackage(o, false, problemsHolder)
      }

      override fun visitCallExpression(psiElement: RCallExpression) {
        if (RPsiUtil.isImportStatement(psiElement)) {
          val info = RArgumentInfo.getArgumentInfo(psiElement)
          val packageArg = info?.getArgumentPassedToParameter("package")
          val characterOnlyArg = info?.getArgumentPassedToParameter("character.only")
          if (packageArg != null) {
            checkPackage(packageArg, isCharacterOnly(characterOnlyArg), problemsHolder)
          }
        }
        psiElement.acceptChildren(this)
      }
    })
  }

  private fun checkPackage(packageExpression: RExpression, isCharacterOnly: Boolean, problemsHolder: ProblemsHolder) {
    // support quoted and unquoted method names here
    val packageName: String?
    val elementForReporting: PsiElement
    when (packageExpression) {
      is RStringLiteralExpression -> {
        packageName = unquote(packageExpression.getText())
        elementForReporting = packageExpression
      }
      is RIdentifierExpression -> {
        if (isCharacterOnly) return
        packageName = packageExpression.getText()
        elementForReporting = packageExpression
      }
      is RNamespaceAccessExpression -> {
        if (isCharacterOnly) return
        packageName = packageExpression.namespaceName
        elementForReporting = packageExpression.namespace
      }
      // could be a function RCallExpression or something weired, so ignore it
      else -> return
    }

    val state = RInterpreterStateManager.getCurrentStateOrNull(packageExpression.project) ?: return
    if (state.isUpdating || packageName == null) {  // Note: also prevents false positives during interpreter's state update
      return
    }

    // Note: don't trigger error if this package is either installed or loaded into global environment
    if (!state.hasPackage(packageName) && !state.rInterop.isLibraryLoaded(packageName)) {
      val descriptionTemplate = RBundle.message("inspection.missingPackage.description", packageName)
      problemsHolder.registerProblem(elementForReporting, descriptionTemplate,
                                     InstallPackagesFix(packageName), InstallAllFilePackagesFix())
    } else {
      checkImplicitDependenciesFor(packageName, elementForReporting, problemsHolder)
    }
  }

  private fun checkImplicitDependenciesFor(packageName: String, element: PsiElement, problemsHolder: ProblemsHolder) {
    findImplicitDependenciesFor(packageName, element.project)?.let { dependencies ->
      val description = createImplicitDependenciesDescription(packageName, dependencies)
      problemsHolder.registerProblem(element, description, ProblemHighlightType.WARNING, InstallPackagesFix(dependencies))
    }
  }

  private fun findImplicitDependenciesFor(packageName: String, project: Project): List<RequiredPackage>? {
    return packageName2Dependencies[packageName]?.let { dependencies ->
      val installer = RequiredPackageInstaller.getInstance(project)
      val missing = installer.getMissingPackages(dependencies)
      missing.takeIf { it.isNotEmpty() }
    }
  }

  private fun isCharacterOnly(characterOnlyArg: RExpression?): Boolean {
    if (characterOnlyArg == null) return false // False by default
    interpretAsBoolean(characterOnlyArg)?.let { return it }
    if (characterOnlyArg is RIdentifierExpression || characterOnlyArg is RNamespaceAccessExpression) {
      val value = (characterOnlyArg.reference?.multiResolve(false)?.firstOrNull()?.element as? RAssignmentStatement)?.assignedValue
      value?.let { interpretAsBoolean(it)?.let { return it } }
    }
    return true // If the calculation failed, it is better to assume that true
  }

  private fun interpretAsBoolean(expression: RExpression): Boolean? {
    return when (expression) {
      is RBooleanLiteral -> expression.isTrue
      is RIdentifierExpression -> when (expression.text) {
        "T" -> true
        "F" -> false
        else -> null
      }
      else -> null
    }
  }

  // http://stackoverflow.com/questions/41298164/how-to-remove-single-and-double-quotes-at-both-ends-of-a-string
  private fun unquote(text: String): String {
    return text.replace("^['\"]*".toRegex(), "").replace("['\"]*$".toRegex(), "")
  }

  companion object {
    private val animateDependencies = listOf("gifski", "png").map { RequiredPackage(it) }

    private val packageName2Dependencies = mapOf(
      "gganimate" to animateDependencies
    )

    @InspectionMessage
    private fun createImplicitDependenciesDescription(packageName: String, dependencies: List<RequiredPackage>): String {
      val dependencyString = dependencies.joinToString(", ") { it.toFormat(true) }
      return RBundle.message("inspection.missingImplicitDependencies.description", packageName, dependencyString)
    }
  }
}
