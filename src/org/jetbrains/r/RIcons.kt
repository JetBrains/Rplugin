/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r

import com.intellij.openapi.util.IconLoader

@JvmField
val R_LOGO_16 = IconLoader.findIcon("/icons/r_logo_16.svg")

object RIcons {
  object Packages {
    val UPGRADE_ALL = IconLoader.findIcon("/icons/packages/upgradeAll.svg")
  }

  object Graphics {
    val CLEAR = IconLoader.findIcon("/icons/graphics/clear.svg")
    val CLEAR_ALL = IconLoader.findIcon("/icons/graphics/clearAll.svg")
    val EXPORT = IconLoader.findIcon("/icons/graphics/export.svg")
  }
}
