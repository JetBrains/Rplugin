/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import org.jetbrains.r.run.graphics.RGraphicsUtils

@State(name = "RMarkdownGraphicsSettings", storages = [Storage("rMarkdownGraphicsSettings.xml")])
class RMarkdownGraphicsSettings(private val project: Project) : SimplePersistentStateComponent<RMarkdownGraphicsSettings.State>(State()) {
  var globalResolution: Int
    get() {
      return with(state) {
        if (globalResolution == 0 || version != CURRENT_VERSION) {
          RGraphicsUtils.getDefaultResolution(false).also { resolution ->
            updateResolution(resolution)
          }
        } else {
          globalResolution
        }
      }
    }
    set(newResolution) {
      updateResolution(newResolution)
    }

  var isStandalone: Boolean
    get() = state.isStandalone
    set(newStandalone) {
      if (state.isStandalone != newStandalone) {
        state.isStandalone = newStandalone
        project.messageBus.syncPublisher(CHANGE_STANDALONE_TOPIC).onStandaloneChange(newStandalone)
      }
    }

  fun addGlobalResolutionListener(parent: Disposable, listener: (Int) -> Unit) {
    project.messageBus.connect(parent).subscribe(CHANGE_GLOBAL_RESOLUTION_TOPIC, object : GlobalResolutionNotifier {
      override fun onGlobalResolutionChange(newResolution: Int) {
        listener(newResolution)
      }
    })
  }

  fun addStandaloneListener(parent: Disposable, listener: (Boolean) -> Unit) {
    project.messageBus.connect(parent).subscribe(CHANGE_STANDALONE_TOPIC, object : StandaloneNotifier {
      override fun onStandaloneChange(isStandalone: Boolean) {
        listener(isStandalone)
      }
    })
  }

  private fun updateResolution(newResolution: Int) {
    state.apply {
      globalResolution = newResolution
      version = CURRENT_VERSION
    }
    project.messageBus.syncPublisher(CHANGE_GLOBAL_RESOLUTION_TOPIC).onGlobalResolutionChange(newResolution)
  }

  class State : BaseState() {
    var globalResolution by property(0)
    var isStandalone by property(true)
    var version by property(0)
  }

  companion object {
    private interface GlobalResolutionNotifier {
      fun onGlobalResolutionChange(newResolution: Int)
    }

    private interface StandaloneNotifier {
      fun onStandaloneChange(isStandalone: Boolean)
    }

    private const val CURRENT_VERSION = 2

    private val CHANGE_GLOBAL_RESOLUTION_TOPIC = Topic.create("R Markdown Global Resolution Topic", GlobalResolutionNotifier::class.java)
    private val CHANGE_STANDALONE_TOPIC = Topic.create("R Standalone GE Topic", StandaloneNotifier::class.java)

    fun getInstance(project: Project) = project.service<RMarkdownGraphicsSettings>()
  }
}
