/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.r.psi.RBundle
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.selected
import java.awt.Component
import javax.swing.*

private const val DEFAULT_RESOLUTION = 75  // It's never required in practice and for backward compatibility purposes only
private const val INPUT_COLUMN_COUNT = 7
private val INPUT_RANGE = IntRange(1, 9999)

class RChunkGraphicsSettingsDialog(
  private val initialSettings: Settings,
  private val onSettingsChange: (Settings) -> Unit
) : DialogWrapper(null, true) {

  private val settings: Settings
    get() = Settings(isAutoResizeEnabled, isDarkModeEnabledOrNull, overridesGlobal, globalResolution, localResolution,
                     globalStandalone, localStandalone)

  private val isDarkModeEnabledOrNull
    get() = isDarkModeEnabled.takeIf { isDarkModeVisible }

  private val isDarkModeVisible
    get() = initialSettings.isDarkModeEnabled != null

  private var isAutoResizeEnabled = initialSettings.isAutoResizedEnabled
  private var isDarkModeEnabled = initialSettings.isDarkModeEnabled ?: false
  private var overridesGlobal = initialSettings.overridesGlobal

  private var localResolution = initialSettings.localResolution ?: DEFAULT_RESOLUTION
  private var globalResolution = initialSettings.globalResolution ?: DEFAULT_RESOLUTION

  private var localStandalone = initialSettings.localStandalone
  private var globalStandalone = initialSettings.globalStandalone

  init {
    isResizable = false
    title = RBundle.message("chunk.graphics.settings.dialog.title")
    init()
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      group(RBundle.message("chunk.graphics.settings.dialog.for.current.plot")) {
        row {
          checkBox(RBundle.message("graphics.panel.settings.dialog.auto.resize"))
            .bindSelected(::isAutoResizeEnabled)
        }
        lateinit var overrideCheckBox: JBCheckBox
        row {
          overrideCheckBox = checkBox(RBundle.message("chunk.graphics.settings.dialog.override.global.text"))
            .bindSelected(::overridesGlobal)
            .component
        }
        row(RBundle.message("graphics.panel.settings.dialog.resolution")) {
          intTextField(INPUT_RANGE)
            .columns(INPUT_COLUMN_COUNT)
            .bindIntText(::localResolution)
            .gap(RightGap.SMALL)
            .enabledIf(overrideCheckBox.selected)
          label(RBundle.message("graphics.panel.settings.dialog.dpi"))
        }
        row(RBundle.message("graphics.panel.engine.text")) {
          comboBox(listOf(true, false), EngineCellRenderer())
            .bindItem(::localStandalone.toNullableProperty())
            .enabledIf(overrideCheckBox.selected)
        }
      }
      group(RBundle.message("chunk.graphics.settings.dialog.for.all.plots")) {
        row(RBundle.message("graphics.panel.settings.dialog.resolution")) {
          intTextField(INPUT_RANGE)
            .columns(INPUT_COLUMN_COUNT)
            .gap(RightGap.SMALL)
            .bindIntText(::globalResolution)
          label(RBundle.message("graphics.panel.settings.dialog.dpi"))
        }
        if (isDarkModeVisible) {
          row {
            checkBox(RBundle.message("chunk.graphics.settings.dialog.adapt.to.dark.theme"))
              .bindSelected(::isDarkModeEnabled)
          }
        }
        row(RBundle.message("graphics.panel.engine.text")) {
          comboBox(listOf(true, false), EngineCellRenderer())
            .bindItem(::globalStandalone.toNullableProperty())
        }
      }
    }
  }

  override fun doOKAction() {
    super.doOKAction()
    onSettingsChange(settings)
  }

  data class Settings(
    val isAutoResizedEnabled: Boolean,
    val isDarkModeEnabled: Boolean?,
    val overridesGlobal: Boolean,
    val globalResolution: Int?,
    val localResolution: Int?,
    val globalStandalone: Boolean,
    val localStandalone: Boolean
  )

  private class EngineCellRenderer : ListCellRenderer<Boolean?> {
    private val delegate = DefaultListCellRenderer()

    override fun getListCellRendererComponent(list: JList<out Boolean?>?,
                                              value: Boolean?,
                                              index: Int,
                                              isSelected: Boolean,
                                              cellHasFocus: Boolean): Component {
      return delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).apply {
        if (this !is JLabel) {
          return@apply
        }
        when (value) {
          true -> {
            text = RBundle.message("graphics.panel.engine.ide.text")
            toolTipText = RBundle.message("graphics.panel.engine.ide.tooltip")
          }
          false -> {
            text = RBundle.message("graphics.panel.engine.r.text")
            toolTipText = RBundle.message("graphics.panel.engine.r.tooltip")
          }
          else -> {
            text = ""
            toolTipText = ""
          }
        }
      }
    }
  }
}
