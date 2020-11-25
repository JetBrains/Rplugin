package org.jetbrains.r.run.configuration

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.openapi.project.guessModuleDir
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import org.jetbrains.r.interpreter.RInterpreterUtil.getDefaultInterpreterOptions
import org.jetbrains.r.psi.api.RFile

class RRunConfigurationProducer : LazyRunConfigurationProducer<RRunConfiguration>() {
  override fun setupConfigurationFromContext(configuration: RRunConfiguration,
                                             context: ConfigurationContext,
                                             sourceElement: Ref<PsiElement>): Boolean {
    val location = context.location ?: return false
    val element = location.psiElement
    val psiFile = element.containingFile ?: return false
    if (psiFile !is RFile || psiFile.viewProvider is TemplateLanguageFileViewProvider) {
      return false
    }

    configuration.name = psiFile.name
    configuration.filePath = psiFile.virtualFile.path

    val moduleDirectory = context.module.guessModuleDir()
    configuration.workingDirectory = moduleDirectory?.path ?: psiFile.virtualFile.parent.path
    if (configuration.interpreterArgs.isEmpty()) {
      configuration.interpreterArgs = getDefaultInterpreterOptions(context.project).joinToString(separator = " ")
    }

    return true
  }

  override fun isConfigurationFromContext(configuration: RRunConfiguration, context: ConfigurationContext): Boolean {
    val location = context.location ?: return false
    val element = location.psiElement
    val psiFile = element.containingFile ?: return false
    if (psiFile !is RFile) {
      return false
    }

    return configuration.filePath == psiFile.virtualFile.path
  }

  override fun getConfigurationFactory() = runConfigurationType<RRunConfigurationType>()
}