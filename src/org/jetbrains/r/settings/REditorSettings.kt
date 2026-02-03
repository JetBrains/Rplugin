/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@State(name = "REditorSettings", storages = [Storage("rGraphicsSettings.xml")])
class REditorSettings : SimplePersistentStateComponent<REditorSettingsState>(REditorSettingsState()) {
  companion object {
    val INSTANCE
      get() = service<REditorSettings>()

    var disableCompletionAutoPopupForShortPrefix: Boolean
      get() = INSTANCE.state.disableCompletionAutoPopupForShortPrefix
      set(value) { INSTANCE.state.disableCompletionAutoPopupForShortPrefix = value }
    var useSoftWrapsInConsole: Boolean
      get() = INSTANCE.state.useSoftWrapsInConsole
      set(value) { INSTANCE.state.useSoftWrapsInConsole = value }
    var useSoftWRapsInRMarkdown: Boolean
      get() = INSTANCE.state.useSoftWrapsInRMarkdown
      set(value) { INSTANCE.state.useSoftWrapsInRMarkdown = value }
  }
}

class REditorSettingsState : BaseState() {
  var useSoftWrapsInConsole: Boolean by property(true)
  var useSoftWrapsInRMarkdown: Boolean by property(true)
  var disableCompletionAutoPopupForShortPrefix: Boolean by property(true)
}