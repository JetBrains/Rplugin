/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.*
import org.intellij.datavis.r.VisualizationBundle
import javax.swing.JComponent

class RChunkGraphicsSettingsDialog(
  private val initialSettings: Settings,
  private val onSettingsChange: (Settings) -> Unit
) : DialogWrapper(null, true) {

  private val settings: Settings
    get() = Settings(isAutoResizeEnabled, isDarkModeEnabledOrNull, globalResolution, localResolution)

  private val isDarkModeEnabledOrNull
    get() = isDarkModeEnabled.takeIf { isDarkModeVisible }

  private val isDarkModeVisible
    get() = initialSettings.isDarkModeEnabled != null

  private var isAutoResizeEnabled = initialSettings.isAutoResizedEnabled
  private var isDarkModeEnabled = initialSettings.isDarkModeEnabled ?: false

  private var localResolution = initialSettings.localResolution ?: DEFAULT_RESOLUTION
  private var globalResolution = initialSettings.globalResolution ?: DEFAULT_RESOLUTION

  init {
    setResizable(false)
    title = TITLE
    init()
  }

  override fun createCenterPanel(): JComponent? {
    val self = this
    return panel {
      titledRow(LOCAL_SETTINGS_TITLE) {
        row {
          checkBox(AUTO_RESIZE_TEXT, self::isAutoResizeEnabled)
        }
        row(RESOLUTION_TEXT) {
          intTextField(self::localResolution, INPUT_COLUMN_COUNT, INPUT_RANGE)
          label(DPI_TEXT)
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
    val globalResolution: Int?,
    val localResolution: Int?
  )

  companion object {
    private const val DEFAULT_RESOLUTION = 75  // It's never required in practice and for backward compatibility purposes only
    private const val INPUT_COLUMN_COUNT = 7
    private val INPUT_RANGE = IntRange(1, 9999)

    private val TITLE = VisualizationBundle.message("graphics.setting.title")
    private val LOCAL_SETTINGS_TITLE = VisualizationBundle.message("graphics.settings.for.current.plot")
    private val AUTO_RESIZE_TEXT = VisualizationBundle.message("graphics.settings.auto.resize")
    private val RESOLUTION_TEXT = VisualizationBundle.message("graphics.settings.resolution")
    private val DPI_TEXT = VisualizationBundle.message("graphics.settings.dpi")

    private val GLOBAL_SETTINGS_TITLE = VisualizationBundle.message("graphics.settings.for.all.plots")
    private val DARK_MODE_TEXT = VisualizationBundle.message("graphics.settings.adapt.to.dark.theme")
  }
}
