/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r

import com.intellij.openapi.util.IconLoader

val R_LOGO_16 = load("r.svg")

val R_MARKDOWN = load("rMarkdown.svg")

object RIcons {
  object Packages {
    val UPGRADE_ALL = load("packages/upgradeAll.svg")
  }
}

private fun load(path: String) = IconLoader.findIcon("/icons/" + path)
