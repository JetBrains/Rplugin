// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.configuration

import com.intellij.execution.Location
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.r.RFileType

// TODO [run][test]
class RRunConfigurationProducer : LazyRunConfigurationProducer<RRunConfiguration>() {
  override fun getConfigurationFactory(): ConfigurationFactory {
    return RRunConfigurationType.instance.mainFactory
  }

  override fun setupConfigurationFromContext(configuration: RRunConfiguration,
                                             context: ConfigurationContext,
                                             sourceElement: Ref<PsiElement>): Boolean {
    val scriptVirtualFile = getScriptVirtualFile(context) ?: return false
    configuration.scriptPath = scriptVirtualFile.path
    configuration.setName(RRunConfigurationUtils.suggestedName(configuration))
    return true
  }

  override fun isConfigurationFromContext(configuration: RRunConfiguration, context: ConfigurationContext): Boolean {
    val scriptVirtualFile = getScriptVirtualFile(context) ?: return false
    val configurationScriptPath = configuration.scriptPath
    val contextScriptPath = scriptVirtualFile.path
    return configurationScriptPath == contextScriptPath
  }

  private fun getScriptVirtualFile(context: ConfigurationContext): VirtualFile? {
    val location = context.location ?: return null
    val psiFile = getRunnablePsiFile(location) ?: return null
    return getPhysicalVirtualFile(psiFile)
  }

  private fun getRunnablePsiFile(location: Location<*>): PsiFile? {
    val result = location.psiElement.containingFile
    return if (result == null || result.fileType !== RFileType) null else result
  }

  private fun getPhysicalVirtualFile(psiFile: PsiFile): VirtualFile? {
    val result = psiFile.virtualFile
    return if (result == null || result is LightVirtualFile) null else result
  }
}
