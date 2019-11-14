/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.help

import com.intellij.openapi.help.WebHelpProvider

class RWebHelpProvider : WebHelpProvider() {
  override fun getHelpPageUrl(helpTopicId: String): String? =
    when (helpTopicId) {
      R_CONSOLE_ID -> "https://www.jetbrains.com/help/pycharm/2019.3/r-plugin-support.html"
      else -> null
    }

  companion object {
    const val R_CONSOLE_ID = "R4Intellij.RConsoleHelp"
  }
}