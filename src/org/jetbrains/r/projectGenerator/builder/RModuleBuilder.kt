package org.jetbrains.r.projectGenerator.builder

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.jetbrains.python.sdk.baseDir
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.projectGenerator.step.RGeneratorSettingsWizardStep
import org.jetbrains.r.projectGenerator.template.RProjectGenerator

class RModuleBuilder(private val generator: RProjectGenerator) : ModuleBuilder() {
  private var settingsWizardStep: RGeneratorSettingsWizardStep? = null

  override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
    doAddContentEntry(modifiableRootModel)
  }

  override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider): Array<ModuleWizardStep> {
    settingsWizardStep = RGeneratorSettingsWizardStep(generator, wizardContext)
    return arrayOf(settingsWizardStep!!)
  }

  override fun getModuleType(): ModuleType<*> = RModuleType.instance

  override fun getName(): String = RBundle.message("module.builder.name")

  override fun getPresentableName(): String = RBundle.message("module.builder.presentable.name")

  override fun getDescription(): String = RBundle.message("module.builder.description")

  override fun getBuilderId(): String = generator.getId()

  override fun commitModule(project: Project, model: ModifiableModuleModel?): Module? {
    ApplicationManager.getApplication().assertIsDispatchThread()
    val module = super.commitModule(project, model)
    if (module != null) {
      val moduleRootManager = ModuleRootManager.getInstance(module)
      val contentRoots = moduleRootManager.contentRoots
      var toStoreDirectory = module.baseDir
      if (contentRoots.isNotEmpty()) {
        toStoreDirectory = contentRoots[0]
      }
      ApplicationManager.getApplication().invokeLater {
        generator.generateProject(project, toStoreDirectory!!, generator.getSettings(), module)
      }
    }
    return module
  }

  override fun validate(current: Project?, dest: Project?): Boolean {
    return settingsWizardStep?.checkValid() ?: true
  }
}