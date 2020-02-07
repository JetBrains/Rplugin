/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi

import com.intellij.ide.util.FileStructureFilter
import com.intellij.ide.util.treeView.smartTree.ActionPresentation
import com.intellij.ide.util.treeView.smartTree.ActionPresentationData
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.openapi.actionSystem.Shortcut
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.util.PlatformIcons
import org.jetbrains.r.RBundle
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RFunctionExpression

const val RGlobalVariablesFilterId = "R_SHOW_GLOBAL_VARIABLES"

class RGlobalVariablesFilter : FileStructureFilter {
  override fun isReverted(): Boolean {
    return true
  }

  override fun getCheckBoxText(): String = RBundle.message("structure.view.global.variable.filter")

  override fun getPresentation(): ActionPresentation {
    return ActionPresentationData(
      RBundle.message("structure.view.global.variable.filter"),
      null,
      PlatformIcons.VARIABLE_ICON)
  }

  override fun getName(): String = RGlobalVariablesFilterId

  override fun getShortcut(): Array<Shortcut> = KeymapUtil.getActiveKeymapShortcuts("FileStructurePopup").getShortcuts()

  override fun isVisible(treeNode: TreeElement?): Boolean {
    if (treeNode !is RStructureViewElement) return true
    val value = treeNode.value
    if (value is RAssignmentStatement && value.assignedValue !is RFunctionExpression) {
      return false
    }
    return true
  }
}