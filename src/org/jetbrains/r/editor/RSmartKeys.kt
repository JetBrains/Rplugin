/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.openapi.options.BeanConfigurable
import org.jetbrains.r.settings.REditorSettings

class RSmartKeys : BeanConfigurable<REditorSettings>(
  REditorSettings.INSTANCE, "R") {
  init {
    checkBox(
      "Disable completion auto-popup for identifier prefixes shorter than 3 symbols",
      { REditorSettings.disableCompletionAutoPopupForShortPrefix },
      { REditorSettings.disableCompletionAutoPopupForShortPrefix = it }
    )
  }
}
