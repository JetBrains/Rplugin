// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.actionSystem.*
import com.intellij.util.ui.JBUI
import javax.swing.Icon
import javax.swing.JPanel

class RGraphicsToolbar(groups: List<ActionHolderGroup>) {
  interface ActionHolder {
    val title: String?
    val description: String
    val icon: Icon?
    val canClick: Boolean

    fun onClick()
  }

  data class ActionHolderGroup(val actionHolders: List<ActionHolder>)

  val component: JPanel

  init {
    component = createToolbar(groups)
  }

  companion object {
    private const val TOOLBAR_PLACE = "Graphics"

    private fun createToolbar(groups: List<ActionHolderGroup>): JPanel {
      class ToolbarAction(private val holder: ActionHolder) : AnAction(holder.title, holder.description, holder.icon) {
        override fun actionPerformed(e: AnActionEvent) {
          holder.onClick()
        }

        override fun update(e: AnActionEvent) {
          e.presentation.isEnabled = holder.canClick
        }

        override fun displayTextInToolbar(): Boolean {
          return holder.title != null
        }
      }

      val actionGroup = DefaultActionGroup().apply {
        for ((index, group) in groups.withIndex()) {
          if (index > 0) {
            addSeparator()
          }
          for (holder in group.actionHolders) {
            add(ToolbarAction(holder))
          }
        }
      }
      val actionToolbar = ActionManager.getInstance().createActionToolbar(TOOLBAR_PLACE, actionGroup, true)
      return JBUI.Panels.simplePanel(actionToolbar.component)
    }

    fun groupOf(vararg holders: ActionHolder) = ActionHolderGroup(listOf(*holders))
  }
}