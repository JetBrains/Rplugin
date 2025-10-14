package org.jetbrains.r.run.configuration

import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.icons.RIcons

private const val ID = "R"

class RRunConfigurationType : SimpleConfigurationType(ID, RBundle.message("r.run.configuration.type.name"), null, NotNullLazyValue.lazy { RIcons.R }) {
  override fun createTemplateConfiguration(project: Project) = RRunConfiguration(project, this)
}