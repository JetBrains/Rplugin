/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.execution.console.ConsoleHistoryController
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor

/** In our code-base *next* means older command in the history. So better use more clear term. */
class RConsoleHistoryOlderCommandAction(private val console: RConsoleView) : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val controller = ConsoleHistoryController.getController(console)
    controller?.historyNext?.actionPerformed(e)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = isEnabled()
  }

  private fun isEnabled(): Boolean {
    val consoleEditor: Editor = console.getCurrentEditor()
    val document = consoleEditor.document
    val caretModel = consoleEditor.caretModel

    if (LookupManager.getActiveLookup(consoleEditor) != null) return false

    // First line or last character
    return document.getLineNumber(caretModel.getOffset()) == 0 || document.textLength == caretModel.offset
  }
}