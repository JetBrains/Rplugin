/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote.ui

import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import javax.swing.JList

class RPackageListCellRenderer : ColoredListCellRenderer<RPackageUpdateInfo>() {
  override fun customizeCellRenderer(
    list: JList<out RPackageUpdateInfo>,
    value: RPackageUpdateInfo?,
    index: Int,
    selected: Boolean,
    hasFocus: Boolean
  ) {
    if (value != null) {
      append(value.installedPackage.name)
      append(" from ", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
      append(value.installedPackage.version, SimpleTextAttributes.GRAYED_ATTRIBUTES)
      append(" to ", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
      append(value.latestVersion, SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }
  }
}
