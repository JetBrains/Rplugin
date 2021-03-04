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

  fun addGlobalResolutionListener(parent: Disposable, listener: (Int) -> Unit) {
    project.messageBus.connect(parent).subscribe(CHANGE_GLOBAL_RESOLUTION_TOPIC, object : GlobalResolutionNotifier {
      override fun onGlobalResolutionChange(newResolution: Int) {
        listener(newResolution)
      }
    })
  }

  private fun updateResolution(newResolution: Int) {
    state.apply {
      version = CURRENT_VERSION
      if (globalResolution != newResolution) {
        globalResolution = newResolution
        project.messageBus.syncPublisher(CHANGE_GLOBAL_RESOLUTION_TOPIC).onGlobalResolutionChange(newResolution)
      }
    }
  }

  class State : BaseState() {
    var globalResolution by property(0)
    var version by property(0)
  }

  companion object {
    private interface GlobalResolutionNotifier {
      fun onGlobalResolutionChange(newResolution: Int)
    }

    private const val CURRENT_VERSION = 2

    private val CHANGE_GLOBAL_RESOLUTION_TOPIC = Topic.create("R Markdown Global Resolution Topic", GlobalResolutionNotifier::class.java)

    fun getInstance(project: Project) = project.service<RMarkdownGraphicsSettings>()
  }
}
