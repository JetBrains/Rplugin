/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.settings

import com.intellij.history.core.Paths
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.project.Project

@State(name="RMarkdownSettings")
class RMarkdownSettings: SimplePersistentStateComponent<RMarkdownSettingsState>(RMarkdownSettingsState()) {
  companion object {
    fun getInstance(project: Project): RMarkdownSettings = ServiceManager.getService(project, RMarkdownSettings::class.java)
  }
}

class RMarkdownSettingsState: BaseState() {
  @Suppress("MemberVisibilityCanBePrivate")
  // it should be public otherwise BaseState cannot save it
  var renderProfiles by map<String, RMarkdownRenderProfile>()

  @Synchronized
  fun getKnitRootDirectory(path: String): String = getOrCreateProfile(path).knitRootDirectory

  @Synchronized
  fun getProfileLastOutput(path: String): String = getOrCreateProfile(path).lastOutput

  @Synchronized
  fun setKnitRootDirectory(path: String, value: String) {
    getOrCreateProfile(path).knitRootDirectory = value
    incrementModificationCount()
  }

  @Synchronized
  fun setProfileLastOutput(path: String, value: String) {
    getOrCreateProfile(path).lastOutput = value
    incrementModificationCount()
  }

  private fun getOrCreateProfile(path: String): RMarkdownRenderProfile {
    val profile = renderProfiles.computeIfAbsent(path) { RMarkdownRenderProfile() }
    return profile.ensureNotBlankDirectory(path)
  }

  private fun RMarkdownRenderProfile.ensureNotBlankDirectory(path: String): RMarkdownRenderProfile {
    return this.also {
      if (it.knitRootDirectory.isBlank()) {
        it.knitRootDirectory = Paths.getParentOf(path)
        incrementModificationCount()
      }
    }
  }
}