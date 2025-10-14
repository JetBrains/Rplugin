/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote.ui

import com.intellij.r.psi.RBundle
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
      appendHint(FROM_HINT)
      append(value.installedPackage.version, SimpleTextAttributes.GRAYED_ATTRIBUTES)
      appendHint(TO_HINT)
      append(value.latestVersion, SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }
  }

  private fun appendHint(hint: String) {
    appendTextGap()
    append(hint, SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
    appendTextGap()
  }

  private fun appendTextGap() {
    append(" ")
  }

  companion object {
    private val FROM_HINT = RBundle.message("package.update.info.cell.renderer.from")
    private val TO_HINT = RBundle.message("package.update.info.cell.renderer.to")
  }
}
