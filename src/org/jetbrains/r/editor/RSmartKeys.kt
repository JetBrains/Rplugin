/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.openapi.options.BeanConfigurable
import com.intellij.r.psi.RBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.r.settings.REditorSettings

@NonNls
private const val TITLE = "R"

class RSmartKeys : BeanConfigurable<REditorSettings>(
  REditorSettings.INSTANCE, TITLE) {
  init {
    checkBox(
      RBundle.message("checkbox.name.disable.completion.auto.popup.for.identifier.prefixes.shorter.than"),
      { REditorSettings.disableCompletionAutoPopupForShortPrefix },
      { REditorSettings.disableCompletionAutoPopupForShortPrefix = it }
    )
  }
}
