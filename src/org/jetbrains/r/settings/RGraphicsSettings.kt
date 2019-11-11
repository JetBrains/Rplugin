/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import org.jetbrains.r.run.graphics.RGraphicsUtils
import java.awt.Dimension

@State(name = "RGraphicsSettings", storages = [Storage("rGraphicsSettings.xml")])
class RGraphicsSettings : SimplePersistentStateComponent<RGraphicsSettingsState>(RGraphicsSettingsState()) {
  companion object {
    fun getInstance(project: Project) = project.service<RGraphicsSettings>()

    fun getScreenParameters(project: Project): RGraphicsUtils.ScreenParameters {
      val state = getInstance(project).state
      val width = state.width
      val height = state.height
      val resolution = state.resolution
      return if (width == 0 || height == 0 || resolution == 0) {
        RGraphicsUtils.getDefaultScreenParameters(false).also {
          setScreenParameters(project, it)
        }
      } else {
        RGraphicsUtils.ScreenParameters(Dimension(width, height), resolution)
      }
    }

    fun setScreenParameters(project: Project, parameters: RGraphicsUtils.ScreenParameters) {
      val state = getInstance(project).state
      state.width = parameters.width
      state.height = parameters.height
      state.resolution = parameters.resolution ?: 0
    }
  }
}

class RGraphicsSettingsState : BaseState() {
  var width: Int by property(0)
  var height: Int by property(0)
  var resolution: Int by property(0)
}
