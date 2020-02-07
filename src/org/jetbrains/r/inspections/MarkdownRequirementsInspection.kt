/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.r.RBundle
import org.jetbrains.r.intentions.InstallLibrariesFix
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.psi.api.RFile
import org.jetbrains.r.rmarkdown.RMarkdownUtil

class MarkdownRequirementsInspection : RInspection() {
  override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
    val problemsHolder = ProblemsHolder(manager, file, isOnTheFly)
    if (file is RFile && file.isMarkdown) {
      checkRMarkdownPackages(file, manager.project, problemsHolder)
    }
    return problemsHolder.resultsArray
  }

  private fun checkRMarkdownPackages(file: PsiFile, project: Project, problemsHolder: ProblemsHolder) {
    if (RInterpreterManager.getInterpreter(project)?.isUpdating == false) {
      RMarkdownUtil.getMissingPackages(project)?.let { missing ->
        if (missing.isNotEmpty()) {
          problemsHolder.registerProblem(file, PROBLEM_DESCRIPTION, InstallLibrariesFix(missing))
        }
      }
    }
  }

  companion object {
    private val PROBLEM_DESCRIPTION = RBundle.message("inspection.markdownRequirements.description")

    private val RFile.isMarkdown: Boolean
      get() = name.substringAfterLast('.').toLowerCase() == "rmd"
  }
}
