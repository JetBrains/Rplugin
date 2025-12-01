package org.jetbrains.r.run.configuration

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.project.guessModuleDir
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.intellij.r.psi.interpreter.RInterpreterUtil.getDefaultInterpreterOptions
import com.intellij.r.psi.interpreter.RLocalInterpreterLocation
import com.intellij.r.psi.psi.api.RFile
import com.intellij.r.psi.settings.RSettings

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

    val dataContext = context.dataContext
    val contextComponent = PlatformCoreDataKeys.CONTEXT_COMPONENT.getData(dataContext)
    if (contextComponent is EditorComponentImpl) {
      return false
    }

    val interpreterLocation = RSettings.getInstance(configuration.project).interpreterLocation
    if (interpreterLocation !is RLocalInterpreterLocation) {
      return false
    }

    configuration.name = psiFile.name
    configuration.filePath = psiFile.virtualFile.path

    val moduleDirectory = context.module?.guessModuleDir()
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