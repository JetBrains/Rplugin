/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.build.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ModalityState.defaultModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager.getInstance
import com.intellij.openapi.project.Project
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.RPluginCoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageInstaller
import com.intellij.r.psi.visualization.ui.ToolbarUtil
import javax.swing.Icon

class RPackageBuildTaskManager(
  private val project: Project,
  private val onReset: () -> Unit,
  private val beforeTask: suspend () -> Unit,
  private val onInterrupted: () -> Unit,
) {
  @Volatile
  private var currentActionHolder: ToolbarUtil.ActionHolder? = null

  @Volatile
  private var isRunning: Boolean = false

  fun createActionHolder(
    id: String,
    task: suspend (Boolean) -> Unit,
    requiredDevTools: Boolean,
    checkVisible: () -> Boolean = { true },
  ) = object : ToolbarUtil.ActionHolder {
    private val missing: List<RequiredPackage>?
      get() = RequiredPackageInstaller.getInstance(project).getMissingPackagesOrNull(REQUIREMENTS)

    private val isRunningMine: Boolean
      get() = currentActionHolder === this

    override val id = id

    private val hasDevTools
      get() = missing?.isEmpty() == true

    override val canClick: Boolean
      get() = (!isRunning || isRunningMine) && (!requiredDevTools || hasDevTools)

    override fun onClick() {
      if (!isRunning) {
        startTask()
      } else {
        onInterrupted()
      }
    }

    override fun checkVisible(): Boolean {
      return checkVisible()
    }

    override fun getHintForDisabled(): String? {
      return if (!isRunning) {
        missing?.let { createMissingPackageMessage(it) }
      } else {
        RBundle.message("packages.build.panel.task.still.running")
      }
    }

    override fun getAlternativeEnabledIcon(): Icon? {
      return AllIcons.Actions.Suspend.takeIf { isRunningMine }
    }

    override fun getAlternativeEnabledDescription(): String? {
      return RBundle.message("packages.build.panel.interrupt.task").takeIf { isRunningMine }
    }

    private fun startTask() {
      onReset()
      isRunning = true
      currentActionHolder = this

      RPluginCoroutineScope.getApplicationScope().launch(defaultModalityState().asContextElement()) {
        try {
          beforeTask()

          edtWriteAction {
            // Note: it's absolutely necessary to flush all the files to a disk
            // in order to make the latest changes take effect on a build task
            getInstance().saveAllDocuments()
          }

          task(hasDevTools)
        }
        finally {
          currentActionHolder = null
          isRunning = false
        }
      }
    }
  }

  companion object {
    private val REQUIREMENTS = listOf(RequiredPackage("devtools"))

    private fun createMissingPackageMessage(missing: List<RequiredPackage>): String {
      val packageString = missing.joinToString { it.toFormat(false) }
      return RBundle.message("required.package.exception.message", packageString)
    }
  }
}
