/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import org.jetbrains.r.RFileType
import org.jetbrains.r.rmarkdown.RMarkdownFileType

/**
 * Marker interface to distinguish R actions invoked in the editor from all other actions.
 */
interface RPromotedAction

class RActionPromoter : ActionPromoter {
  override fun promote(actions: MutableList<AnAction>, context: DataContext): List<AnAction> {
    if (context.getData(CommonDataKeys.VIRTUAL_FILE)?.fileType.let { it != RFileType && it != RMarkdownFileType }) {
      return emptyList<AnAction>()
    }
    @Suppress("SimplifiableCall")
    return actions.filter { it is RPromotedAction }
  }
}