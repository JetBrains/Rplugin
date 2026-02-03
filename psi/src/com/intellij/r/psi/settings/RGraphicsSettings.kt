/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.r.psi.run.graphics.RGraphicsUtils
import com.intellij.r.psi.visualization.inlays.components.CHANGE_DARK_MODE_TOPIC
import com.intellij.r.psi.visualization.inlays.components.DarkModeNotifier
import com.intellij.util.messages.Topic
import java.awt.Dimension

@Service(Service.Level.PROJECT)
@State(name = "RGraphicsSettings", storages = [Storage("rGraphicsSettings.xml")])
class RGraphicsSettings : SimplePersistentStateComponent<RGraphicsSettingsState>(RGraphicsSettingsState()) {
  companion object {
    private interface StandaloneNotifier {
      fun onStandaloneChange(isStandalone: Boolean)
    }

    private const val CURRENT_VERSION = 2

    private val CHANGE_STANDALONE_TOPIC = Topic.create("R Standalone GE Topic", StandaloneNotifier::class.java)

    fun getInstance(project: Project) = project.service<RGraphicsSettings>()

    fun getScreenParameters(project: Project): RGraphicsUtils.ScreenParameters {
      return with(getInstance(project).state) {
        if (width == 0 || height == 0 || resolution == 0 || version != CURRENT_VERSION) {
          RGraphicsUtils.getDefaultScreenParameters(false).also {
            setScreenParameters(project, it)
          }
        } else {
          RGraphicsUtils.ScreenParameters(Dimension(width, height), resolution)
        }
      }
    }

    fun setScreenDimension(project: Project, dimension: Dimension) {
      setScreenParameters(project, dimension, null)
    }

    fun setScreenParameters(project: Project, parameters: RGraphicsUtils.ScreenParameters) {
      setScreenParameters(project, parameters.dimension, parameters.resolution ?: 0)
    }

    fun isDarkModeEnabled(project: Project): Boolean =
      getInstance(project).state.darkMode

    fun setDarkMode(project: Project, isEnabled: Boolean) {
      val state = getInstance(project).state
      if (state.darkMode != isEnabled) {
        state.darkMode = isEnabled
        project.messageBus.syncPublisher(CHANGE_DARK_MODE_TOPIC).onDarkModeChanged(isEnabled)
      }
    }

    fun isStandalone(project: Project): Boolean {
      return getInstance(project).state.isStandalone
    }

    fun setStandalone(project: Project, value: Boolean) {
      val state = getInstance(project).state
      if (state.isStandalone != value) {
        state.isStandalone = value
        project.messageBus.syncPublisher(CHANGE_STANDALONE_TOPIC).onStandaloneChange(value)
      }
    }

    fun getImageNumber(project: Project): Int {
      return getInstance(project).state.imageNumber
    }

    fun setImageNumber(project: Project, number: Int) {
      getInstance(project).state.imageNumber = number
    }

    fun getOutputDirectory(project: Project): String? {
      return getInstance(project).state.outputDirectory
    }

    fun setOutputDirectory(project: Project, directory: String?) {
      getInstance(project).state.outputDirectory = directory
    }

    fun addDarkModeListener(project: Project, parent: Disposable, listener: (Boolean) -> Unit) {
      project.messageBus.connect(parent).subscribe(CHANGE_DARK_MODE_TOPIC, object : DarkModeNotifier {
        override fun onDarkModeChanged(isEnabled: Boolean) {
          listener(isEnabled)
        }
      })
    }

    fun addStandaloneListener(project: Project, parent: Disposable, listener: (Boolean) -> Unit) {
      project.messageBus.connect(parent).subscribe(CHANGE_STANDALONE_TOPIC, object : StandaloneNotifier {
        override fun onStandaloneChange(isStandalone: Boolean) {
          listener(isStandalone)
        }
      })
    }
    
    private fun setScreenParameters(project: Project, dimension: Dimension, resolutionMaybe: Int?) {
      getInstance(project).state.apply {
        width = dimension.width
        height = dimension.height
        if (resolutionMaybe != null) {
          resolution = resolutionMaybe
        }
        version = CURRENT_VERSION
      }
    }
  }
}

class RGraphicsSettingsState : BaseState() {
  var width: Int by property(0)
  var height: Int by property(0)
  var resolution: Int by property(0)
  var version: Int by property(0)
  var imageNumber: Int by property(0)
  var outputDirectory: String? by string()
  var darkMode: Boolean by property(true)
  var isStandalone: Boolean by property(true)
}
