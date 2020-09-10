/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.configuration

import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TitledSeparator
import com.intellij.util.ui.JBUI
import icons.RIcons
import org.jetbrains.r.RBundle
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RLocalInterpreterLocation
import org.jetbrains.r.interpreter.R_UNKNOWN
import java.awt.Component
import javax.swing.JList

class RInterpreterListCellRenderer : ColoredListCellRenderer<Any>() {
  override fun getListCellRendererComponent(
    list: JList<out Any>?,
    value: Any?,
    index: Int,
    selected: Boolean,
    hasFocus: Boolean
  ): Component {
    return when (value) {
      SEPARATOR -> TitledSeparator(null).apply {
        border = JBUI.Borders.empty()
      }
      else -> super.getListCellRendererComponent(list, value, index, selected, hasFocus)
    }
  }

  override fun customizeCellRenderer(list: JList<out Any>, value: Any?, index: Int, selected: Boolean, hasFocus: Boolean) {
    when (value) {
      is RInterpreterInfo -> {
        appendName(value)
        icon = RIcons.R
      }
      null -> {
        appendName(null)
        icon = null
      }
    }
  }

  private fun appendName(interpreter: RInterpreterInfo?) {
    if (interpreter != null) {
      append(interpreter.interpreterName)
      append(" (${interpreter.version}) ")
      append(interpreter.interpreterLocation.toString(), SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
    } else {
      append(NO_INTERPRETER)
    }
  }

  companion object {
    private val NO_INTERPRETER = RBundle.message("project.settings.cell.renderer.no.interpreter")

    val SEPARATOR = object : RInterpreterInfo {
      override val interpreterName = ""
      override val interpreterLocation = RLocalInterpreterLocation("")
      override val version = R_UNKNOWN
    }
  }
}