/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize.actions

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import icons.RIcons
import org.jetbrains.r.RBundle
import org.jetbrains.r.console.RConsoleManager

class RImportDataContextActionGroup : ActionGroup(TITLE, null, RIcons.R_logo_16), DumbAware {
  private val actions: Array<RImportDataContextAction> = arrayOf(
    RImportBaseDataContextAction(),
    RImportCsvDataContextAction(),
    RImportExcelDataContextAction()
  )

  override fun getChildren(e: AnActionEvent?): Array<AnAction> {
    val applicable = if (e != null) actions.filter { it.isApplicableTo(e) } else null
    return applicable?.toTypedArray<AnAction>() ?: emptyArray()
  }

  override fun update(e: AnActionEvent) {
    val hasConsole = e.project?.let { RConsoleManager.getInstance(it).currentConsoleOrNull } != null
    e.presentation.isEnabledAndVisible = hasConsole && actions.any { it.isApplicableTo(e) }
  }

  companion object {
    private val TITLE = RBundle.message("import.data.action.group.name.from")
  }
}
