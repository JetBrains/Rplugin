/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package icons.org.jetbrains.r.packages.remote.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.packages.RInstalledPackage
import org.jetbrains.r.ui.RDimensionPreference
import org.jetbrains.r.ui.calculateDialogPreferredSize
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

  override fun createCenterPanel(): JComponent? {
    return JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      add(JLabel(DESCRIPTION))
      add(Box.createRigidArea(Dimension(0, 10)))
      add(scrollPane)
      preferredSize = calculateDialogPreferredSize(RDimensionPreference.NARROW, RDimensionPreference.MODERATE)
    }
  }

  override fun doOKAction() {
    super.doOKAction()
    onConfirm()
  }

  companion object {
    private val TITLE = RBundle.message("packages.panel.upgrade.all.dialog.title")
    private val DESCRIPTION = RBundle.message("packages.panel.upgrade.all.dialog.description")
  }
}
