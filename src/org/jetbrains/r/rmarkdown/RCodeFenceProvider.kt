/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.parser.LookaheadText
import org.intellij.markdown.parser.MarkerProcessor
import org.intellij.markdown.parser.ProductionHolder
import org.intellij.markdown.parser.constraints.MarkdownConstraints
import org.intellij.markdown.parser.markerblocks.MarkerBlock
import org.intellij.markdown.parser.markerblocks.MarkerBlockProvider
import org.intellij.markdown.parser.markerblocks.impl.CodeFenceMarkerBlock
import org.intellij.markdown.parser.sequentialparsers.SequentialParser

/**
 * It is a copy-paste, only regexp is changed
 *
 * @see org.intellij.markdown.parser.markerblocks.providers.CodeFenceProvider
 */
class RCodeFenceProvider : MarkerBlockProvider<MarkerProcessor.StateInfo> {
  override fun createMarkerBlocks(pos: LookaheadText.Position,
                                  productionHolder: ProductionHolder,
                                  stateInfo: MarkerProcessor.StateInfo): List<MarkerBlock> {
    val fenceAndInfo = getFenceStartAndInfo(pos, stateInfo.currentConstraints)
    if (fenceAndInfo != null) {
      createNodesForFenceStart(pos, fenceAndInfo, productionHolder)
      return listOf(CodeFenceMarkerBlock(stateInfo.currentConstraints, productionHolder, fenceAndInfo.first))
    } else {
      return emptyList()
    }
  }

  override fun interruptsParagraph(pos: LookaheadText.Position, constraints: MarkdownConstraints): Boolean {
    return getFenceStartAndInfo(pos, constraints) != null
  }

  private fun createNodesForFenceStart(pos: LookaheadText.Position, fenceAndInfo: Pair<String, String>, productionHolder: ProductionHolder) {
    val infoStartPosition = pos.nextLineOrEofOffset - fenceAndInfo.second.length
    productionHolder.addProduction(listOf(SequentialParser.Node(pos.offset..infoStartPosition, MarkdownTokenTypes.CODE_FENCE_START)))
    if (fenceAndInfo.second.length > 0) {
      productionHolder.addProduction(listOf(SequentialParser.Node(infoStartPosition..pos.nextLineOrEofOffset, MarkdownTokenTypes.FENCE_LANG)))
    }
  }

  private fun getFenceStartAndInfo(pos: LookaheadText.Position, constraints: MarkdownConstraints): Pair<String, String>? {
    if (!MarkerBlockProvider.isStartOfLineWithConstraints(pos, constraints)) {
      return null
    }
    val matchResult = REGEX.find(pos.currentLineFromPosition)
                      ?: return null
    return Pair(matchResult.groups[1]?.value!!, matchResult.groups[2]?.value!!)
  }

  companion object {
    val REGEX: Regex = Regex("\\A {0,3}(~~~+|```+)(([^`]*)|(\\{[^}]*}[ \t]*))$")
  }
}