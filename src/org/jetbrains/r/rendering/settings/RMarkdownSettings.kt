/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager

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
  fun getOutputDirectory(file: VirtualFile): VirtualFile? {
    return VirtualFileManager.getInstance().findFileByUrl(getOrCreateProfile(file).outputDirectoryUrl)
  }

  @Synchronized
  fun getProfileLastOutput(file: VirtualFile) = getOrCreateProfile(file).lastOutput

  @Synchronized
  fun setOutputDirectory(file: VirtualFile, value: VirtualFile?) {
    getOrCreateProfile(file).outputDirectoryUrl = value?.url.orEmpty()
    incrementModificationCount()
  }

  @Synchronized
  fun setProfileLastOutput(file: VirtualFile, value: String) {
    getOrCreateProfile(file).lastOutput = value
    incrementModificationCount()
  }

  private fun getOrCreateProfile(file: VirtualFile): RMarkdownRenderProfile {
    val profile = renderProfiles.computeIfAbsent(file.url) { RMarkdownRenderProfile() }
    return profile.ensureNotBlankDirectory(file)
  }

  private fun RMarkdownRenderProfile.ensureNotBlankDirectory(file: VirtualFile): RMarkdownRenderProfile {
    return this.also {
      if (it.outputDirectoryUrl.isBlank()) {
        it.outputDirectoryUrl = file.parent.url
        incrementModificationCount()
      }
    }
  }
}