/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.*
import org.jetbrains.r.RBundle
import javax.swing.JComponent

class RChunkGraphicsSettingsDialog(
  private val initialSettings: Settings,
  private val onSettingsChange: (Settings) -> Unit
) : DialogWrapper(null, true) {

  private val settings: Settings
    get() = Settings(isAutoResizeEnabled, isDarkModeEnabledOrNull, globalResolution, localResolution, globalStandalone, localStandalone)

  private val isDarkModeEnabledOrNull
    get() = isDarkModeEnabled.takeIf { isDarkModeVisible }

  private val isDarkModeVisible
    get() = initialSettings.isDarkModeEnabled != null

  private var isAutoResizeEnabled = initialSettings.isAutoResizedEnabled
  private var isDarkModeEnabled = initialSettings.isDarkModeEnabled ?: false

  private var localResolution = initialSettings.localResolution ?: DEFAULT_RESOLUTION
  private var globalResolution = initialSettings.globalResolution ?: DEFAULT_RESOLUTION

  private var localStandalone = initialSettings.localStandalone
  private var globalStandalone = initialSettings.globalStandalone

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
        row {
          checkBox(STANDALONE_TEXT, self::localStandalone)
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
        row {
          checkBox(STANDALONE_TEXT, self::globalStandalone, STANDALONE_COMMENT)
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
    val localResolution: Int?,
    val globalStandalone: Boolean,
    val localStandalone: Boolean
  )

  companion object {
    private const val DEFAULT_RESOLUTION = 75  // It's never required in practice and for backward compatibility purposes only
    private const val INPUT_COLUMN_COUNT = 7
    private val INPUT_RANGE = IntRange(1, 9999)

    private val TITLE = RBundle.message("chunk.graphics.settings.dialog.title")
    private val LOCAL_SETTINGS_TITLE = RBundle.message("chunk.graphics.settings.dialog.for.current.plot")
    private val AUTO_RESIZE_TEXT = RBundle.message("graphics.panel.settings.dialog.auto.resize")
    private val RESOLUTION_TEXT = RBundle.message("graphics.panel.settings.dialog.resolution")
    private val DPI_TEXT = RBundle.message("graphics.panel.settings.dialog.dpi")

    private val GLOBAL_SETTINGS_TITLE = RBundle.message("chunk.graphics.settings.dialog.for.all.plots")
    private val DARK_MODE_TEXT = RBundle.message("chunk.graphics.settings.dialog.adapt.to.dark.theme")

    private val STANDALONE_TEXT = RBundle.message("graphics.panel.settings.dialog.standalone.text")
    private val STANDALONE_COMMENT = RBundle.message("graphics.panel.settings.dialog.standalone.comment")
  }
}
