/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.template

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.r.psi.RBundle
import org.jetbrains.r.actions.NewRScriptAction

class REmptyProjectGenerator : RProjectGenerator() {
  override fun getName(): String {
      return RBundle.message("project.generator.empty.name")
  }

  override fun getDescription(): String {
    return RBundle.message("project.generator.empty.description")
  }

  override fun getId(): String {
    return "R_PROJECT"
  }

  override fun generateProject(project: Project, baseDir: VirtualFile, rProjectSettings: RProjectSettings, module: Module) {
    super.generateProject(project, baseDir, rProjectSettings, module)
    StartupManager.getInstance(project).runWhenProjectIsInitialized {
      ApplicationManager.getApplication().invokeLater {
        runWriteAction {
          val psiBaseDir = PsiManager.getInstance(project).findDirectory(baseDir) ?: return@runWriteAction
          val template = FileTemplateManager.getInstance(project).getInternalTemplate(NewRScriptAction.NEW_R_SCRIPT_TEMPLATE_NAME)
          val psiFile = try {
            FileTemplateUtil.createFromTemplate(template, "main.R", null, psiBaseDir) as PsiFile
          }
          catch (ignore: Exception) {
            null
          }

          psiFile?.navigate(true)
        }
      }
    }
  }
}