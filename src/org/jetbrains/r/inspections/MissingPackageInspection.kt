// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.intentions.InstallAllFileLibraryFix
import org.jetbrains.annotations.Nls
import org.jetbrains.r.intentions.InstallLibraryFix
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.RRecursiveElementVisitor
import org.jetbrains.r.psi.api.*

class MissingPackageInspection : RInspection() {

  @Nls
  override fun getDisplayName(): String {
    return RBundle.message("missing.package.inspection.name")
  }

  override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
    val problemsHolder = ProblemsHolder(manager, file, isOnTheFly)
    checkFile(file, problemsHolder)
    return problemsHolder.resultsArray
  }

  private fun checkFile(file: PsiFile, problemsHolder: ProblemsHolder) {
    if (file !is RFile) return

    file.accept(object : RRecursiveElementVisitor() {
      override fun visitNamespaceAccessExpression(o: RNamespaceAccessExpression) {
        checkPackage(o, problemsHolder)
      }

      override fun visitCallExpression(psiElement: RCallExpression) {
        if (RPsiUtil.isImportStatement(psiElement) && psiElement.argumentList.expressionList.isNotEmpty()) {
          val packageExpression = psiElement.argumentList.expressionList[0]
          checkPackage(packageExpression, problemsHolder)
        }
        psiElement.acceptChildren(this)
      }
    })
  }

  private fun checkPackage(packageExpression: RExpression, problemsHolder: ProblemsHolder) {
    // support quoted and unquoted method names here
    val packageName: String?
    val elementForReporting: PsiElement
    when (packageExpression) {
      is RStringLiteralExpression -> {
        packageName = unquote(packageExpression.getText())
        elementForReporting = packageExpression
      }
      is RIdentifierExpression -> {
        packageName = packageExpression.getText()
        elementForReporting = packageExpression
      }
      is RNamespaceAccessExpression -> {
        packageName = packageExpression.namespaceName
        elementForReporting = packageExpression.namespace
      }
      // could be a function RCallExpression or something weired, so ignore it
      else -> return
    }

    val rInterpreter = RInterpreterManager.getInterpreter(packageExpression.project)
    if (rInterpreter == null || packageName == null) {
      return
    }
    val byName = rInterpreter.getPackageByName(packageName)

    if (byName == null) {
      val descriptionTemplate = RBundle.message("missing.package.inspection.description", packageName)
      problemsHolder.registerProblem(elementForReporting, descriptionTemplate,
                                     InstallLibraryFix(packageName), InstallAllFileLibraryFix())
    }
  }

  // http://stackoverflow.com/questions/41298164/how-to-remove-single-and-double-quotes-at-both-ends-of-a-string
  private fun unquote(text: String): String {
    return text.replace("^['\"]*".toRegex(), "").replace("['\"]*$".toRegex(), "")
  }
}
