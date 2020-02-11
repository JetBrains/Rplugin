/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.rmarkdown.RMarkdownFileType


/**
 * Action to create a new RMarkdown file from a template.
 * <br></br>
 * The template data is stored in resources/fileTemplates/internal/RMarkdown.rmd.ft
 */
class NewRMarkdownAction : TestableCreateFileFromTemplateAction("RMarkdown file",
                                                                "Creates a new RMarkdown file",
                                                                RMarkdownFileType.icon), DumbAware {

  override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
    builder
      .setTitle("New RMarkdown file")
      .addKind("RMarkdown Notebook", RMarkdownFileType.icon, NOTEBOOK_TEMPLATE_NAME)
      .addKind("RMarkdown Document", RMarkdownFileType.icon, DOCUMENT_TEMPLATE_NAME)
      .addKind("RMarkdown Presentation", RMarkdownFileType.icon, PRESENTATION_TEMPLATE_NAME)
      .addKind("RMarkdown Shiny", RMarkdownFileType.icon, SHINY_TEMPLATE_NAME)
  }

  override fun getActionName(directory: PsiDirectory, newName: String, templateName: String): String {
    return "RMarkdown file"
  }

  override fun createFile(name: String?, templateName: String, directory: PsiDirectory): PsiFile? {
    RInterpreterManager.getInstance(directory.project).initializeInterpreter()
    return super.createFile(name, templateName, directory)
  }

  companion object {
    const val NOTEBOOK_TEMPLATE_NAME = "RMarkdown.notebook"
    const val DOCUMENT_TEMPLATE_NAME = "RMarkdown.document"
    const val PRESENTATION_TEMPLATE_NAME = "RMarkdown.presentation"
    const val SHINY_TEMPLATE_NAME = "RMarkdown.shiny"
  }
}
