/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.packages.RInstalledPackage
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import org.jetbrains.annotations.Nls
import org.jetbrains.r.visualization.inlays.components.DialogUtil
import java.awt.Dimension
import javax.swing.*

data class RPackageUpdateInfo(
  val installedPackage: RInstalledPackage,
  val latestVersion: String
)

class RUpdateAllConfirmDialog(outdated: List<RPackageUpdateInfo>, private val onConfirm: () -> Unit) :
  DialogWrapper(null, true)
{
  private val outdatedList = JBList(outdated).apply {
    cellRenderer = RPackageListCellRenderer()
    addListSelectionListener {
      if (selectedValue != null) {
        setSelectedValue(null, false)
      }
    }
  }
  private val scrollPane = JBScrollPane(outdatedList)

  init {
    title = TITLE
    init()
  }

  override fun createCenterPanel(): JComponent {
    return JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      add(JLabel(DESCRIPTION))
      add(Box.createRigidArea(Dimension(0, 10)))
      add(scrollPane)
      preferredSize = DialogUtil.calculatePreferredSize(DialogUtil.SizePreference.NARROW, DialogUtil.SizePreference.MODERATE)
    }
  }

  override fun doOKAction() {
    super.doOKAction()
    onConfirm()
  }

  companion object {
    @Nls
    private val TITLE = RBundle.message("packages.panel.upgrade.all.dialog.title")
    @Nls
    private val DESCRIPTION = RBundle.message("packages.panel.upgrade.all.dialog.description")
  }
}
