// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.RFileType
import com.intellij.r.psi.interpreter.RInterpreterManager


/**
 * Action to create a new R script from a template.
 * <br></br>
 * The template data is stored in resources/fileTemplates/internal/R Script.R.ft
 */
class NewRScriptAction : TestableCreateFileFromTemplateAction(), DumbAware {

  override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
    builder
      .setTitle(RBundle.message("create.r.script.title"))
      .addKind(RBundle.message("create.r.script.text"), RFileType.icon, NEW_R_SCRIPT_TEMPLATE_NAME)
  }

  override fun getActionName(directory: PsiDirectory, newName: String, templateName: String): String {
    return RBundle.message("create.r.script.name")
  }

  override fun createFile(name: String?, templateName: String, directory: PsiDirectory): PsiFile? {
    RInterpreterManager.getInstance(directory.project).launchInterpreter()
    return super.createFile(name, templateName, directory)
  }

  companion object {
    const val NEW_R_SCRIPT_TEMPLATE_NAME = "R Script"
  }
}