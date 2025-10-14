/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.r.psi.RBundle
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import org.jetbrains.r.packages.remote.RMirror
import org.jetbrains.r.visualization.inlays.components.DialogUtil
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel

class RChooseMirrorDialog(mirrors: List<RMirror>, selection: Int, private val onSelected: (Int) -> Unit)
  : DialogWrapper(null, true)
{
  private val mirrorList = JBList(mirrors).apply {
    cellRenderer = RMirrorListCellRenderer()
    selectionMode = ListSelectionModel.SINGLE_SELECTION
    selectedIndex = selection
  }
  private val scrollPane = JBScrollPane(mirrorList)

  init {
    title = RBundle.message("repo.dialog.choose.mirror.title")
    init()
  }

  override fun createCenterPanel(): JComponent {
    return JPanel().apply {
      layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
      add(scrollPane)
      preferredSize = DialogUtil.calculatePreferredSize(DialogUtil.SizePreference.NARROW, DialogUtil.SizePreference.WIDE)
    }
  }

  override fun doOKAction() {
    super.doOKAction()
    val selection = mirrorList.selectedIndex
    if (selection >= 0) {
      onSelected(selection)
    }
  }
}
