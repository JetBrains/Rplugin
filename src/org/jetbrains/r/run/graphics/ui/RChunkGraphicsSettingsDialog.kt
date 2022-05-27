/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.*
import org.jetbrains.r.RBundle
import java.awt.Component
import javax.swing.*

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

  private val localComboBoxModel = CollectionComboBoxModel(listOf(true, false), localStandalone)
  private val globalComboBoxModel = CollectionComboBoxModel(listOf(true, false), globalStandalone)

  init {
    setResizable(false)
    title = TITLE
    init()
  }

  override fun createCenterPanel(): JComponent {
    val self = this
    return panel {
      titledRow(LOCAL_SETTINGS_TITLE) {
        row {
          checkBox(AUTO_RESIZE_TEXT, self::isAutoResizeEnabled)
        }
        val overrideCheckBox = JBCheckBox(OVERRIDE_GLOBAL_TEXT, overridesGlobal)
        row {
          overrideCheckBox().withSelectedBinding(self::overridesGlobal.toBinding())
        }
        row(RESOLUTION_TEXT) {
          intTextField(self::localResolution, INPUT_COLUMN_COUNT, INPUT_RANGE).enableIf(overrideCheckBox.selected)
          label(DPI_TEXT)
        }
        row(ENGINE_TEXT) {
          comboBox(localComboBoxModel, self::localStandalone, EngineCellRenderer()).enableIf(overrideCheckBox.selected)
        }
      }
      titledRow(GLOBAL_SETTINGS_TITLE) {
        row(RESOLUTION_TEXT) {
          intTextField(self::globalResolution, INPUT_COLUMN_COUNT, INPUT_RANGE)
          label(DPI_TEXT)
        }
        if (isDarkModeVisible) {
          row {
            checkBox(DARK_MODE_TEXT, self::isDarkModeEnabled)
          }
        }
        row(ENGINE_TEXT) {
          comboBox(globalComboBoxModel, self::globalStandalone, EngineCellRenderer())
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
            text = ENGINE_IDE_TEXT
            toolTipText = ENGINE_IDE_TOOLTIP
          }
          false -> {
            text = ENGINE_R_TEXT
            toolTipText = ENGINE_R_TOOLTIP
          }
          else -> {
            text = ""
            toolTipText = ""
          }
        }
      }
    }
  }

  companion object {
    private const val DEFAULT_RESOLUTION = 75  // It's never required in practice and for backward compatibility purposes only
    private const val INPUT_COLUMN_COUNT = 7
    private val INPUT_RANGE = IntRange(1, 9999)

    private val TITLE = RBundle.message("chunk.graphics.settings.dialog.title")
    private val OVERRIDE_GLOBAL_TEXT = RBundle.message("chunk.graphics.settings.dialog.override.global.text")
    private val LOCAL_SETTINGS_TITLE = RBundle.message("chunk.graphics.settings.dialog.for.current.plot")
    private val AUTO_RESIZE_TEXT = RBundle.message("graphics.panel.settings.dialog.auto.resize")
    private val RESOLUTION_TEXT = RBundle.message("graphics.panel.settings.dialog.resolution")
    private val DPI_TEXT = RBundle.message("graphics.panel.settings.dialog.dpi")

    private val GLOBAL_SETTINGS_TITLE = RBundle.message("chunk.graphics.settings.dialog.for.all.plots")
    private val DARK_MODE_TEXT = RBundle.message("chunk.graphics.settings.dialog.adapt.to.dark.theme")

    private val ENGINE_TEXT = RBundle.message("graphics.panel.engine.text")
    private val ENGINE_IDE_TEXT = RBundle.message("graphics.panel.engine.ide.text")
    private val ENGINE_IDE_TOOLTIP = RBundle.message("graphics.panel.engine.ide.tooltip")
    private val ENGINE_R_TEXT = RBundle.message("graphics.panel.engine.r.text")
    private val ENGINE_R_TOOLTIP = RBundle.message("graphics.panel.engine.r.tooltip")
  }
}
