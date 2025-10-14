// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor.formatting

import com.intellij.formatting.Block
import com.intellij.formatting.BlockEx
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.psi.formatter.FormatterUtil.isWhitespaceOrEmpty
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.r.psi.RLanguage

class RFormatterBlock internal constructor(private val context: RFormattingContext, node: ASTNode)
  : AbstractBlock(node, context.computeWrap(node), context.computeAlignment(node)), BlockEx {
  override fun buildChildren(): List<RFormatterBlock> =
    node.getChildren(null).mapNotNull {
      if (isWhitespaceOrEmpty(it) || it.textLength == 0) null else RFormatterBlock(context, it)
    }

  override fun getIndent(): Indent? = context.computeBlockIndent(node)

  override fun getSpacing(child1: Block?, child2: Block): Spacing? = context.computeSpacing(this, child1, child2)

  override fun getChildIndent(): Indent = context.computeNewChildIndent(node)

//  override fun isIncomplete(): Boolean = context.isIncomplete(node)

  override fun isLeaf(): Boolean = node.firstChildNode == null

  override fun getLanguage(): Language = RLanguage.INSTANCE
}
