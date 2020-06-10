/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.panel.packageManager

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.webcore.packaging.PackageManagementService
import com.intellij.webcore.packaging.PackagesNotificationPanel
import org.jetbrains.r.RBundle
import org.jetbrains.r.execution.ExecuteExpressionUtils
import org.jetbrains.r.packages.build.RPackageBuildUtil
import org.jetbrains.r.projectGenerator.panel.RPanel
import org.jetbrains.r.projectGenerator.template.RProjectSettings
import java.nio.file.Paths

abstract class RPackageManagerPanel(private val rProjectSettings: RProjectSettings) : RPanel() {
  abstract val packageManagerName: String
  protected abstract val initProjectScriptName: String
  protected open val willCreateDescription = true

  open val rPackageName by lazy { packageManagerName }
  protected open val initializingTitle by lazy { RBundle.message("init.project.title", packageManagerName) }
  protected open val initializingIndicatorText by lazy { RBundle.message("init.project.indicator.text", packageManagerName) }
  private var isPackageInstalledAction: (Boolean) -> Unit = {}

  abstract fun generateProject(project: Project, baseDir: VirtualFile, module: Module)

  protected val relativeScriptPath: String
    get() = Paths.get("projectGenerator", "$initProjectScriptName.R").toString()

  protected fun reportPackageInstallationFailure(message: String, solution: String? = null) {
    ApplicationManager.getApplication().invokeLater {
      PackagesNotificationPanel.showError(RBundle.message("init.project.failed.title", packageManagerName),
                                          PackageManagementService.ErrorDescription(message, null, null, solution))
    }
  }

  open fun validateSettings(): List<ValidationInfo> {
    return if (rProjectSettings.installedPackages.isEmpty() || rProjectSettings.installedPackages.contains(rPackageName)) {
      runIsPackageInstalledAction(true)
      emptyList()
    }
    else {
      runIsPackageInstalledAction(false)
      listOf(ValidationInfo(RBundle.message("project.setting.missing.package.manager", rPackageName)))
    }
  }

  protected fun initializePackage(project: Project, baseDir: VirtualFile, args: List<String>) {
    if (willCreateDescription) {
      RPackageBuildUtil.markAsPackage(project)
    }
    ProgressManager.getInstance().run(
      object : Task.Modal(project, initializingTitle, false) {
        override fun run(indicator: ProgressIndicator) {
          indicator.text = initializingIndicatorText
          val result = ExecuteExpressionUtils.executeScript(rProjectSettings.interpreterLocation!!, relativeScriptPath, args)
          if (result.exitCode != 0) {
            reportPackageInstallationFailure("${result.stdout.dropWhile { it != '\n' }.drop(1)}\n${result.stderr}")
          }
          baseDir.fileSystem.refresh(false)
        }
      })
  }

  protected fun focusFile(project: Project, baseDir: VirtualFile, fileName: String) {
    StartupManager.getInstance(project).runWhenProjectIsInitialized {
      ApplicationManager.getApplication().invokeLater {
        val psiBaseDir = PsiManager.getInstance(project).findDirectory(baseDir) ?: return@invokeLater
        val psiFile = psiBaseDir.findFile(fileName) ?: return@invokeLater
        psiFile.navigate(true)
      }
    }
  }

  protected fun runIsPackageInstalledAction(isPackageInstalled: Boolean) {
    return isPackageInstalledAction(isPackageInstalled)
  }

  fun setIsPackageInstalledAction(newAction: (Boolean) -> Unit) {
    isPackageInstalledAction = newAction
  }

  companion object {
    const val RCONSOLE_WAITING_TIME = 5000
    val RCONSOLE_WAITING_TITLE = RBundle.message("init.project.waiting.title")
  }
}