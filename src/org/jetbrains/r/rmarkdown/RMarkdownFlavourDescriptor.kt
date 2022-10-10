/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.flavours.commonmark.CommonMarkMarkerProcessor
import org.intellij.markdown.flavours.gfm.GFMConstraints
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.flavours.gfm.table.GitHubTableMarkerProvider
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.lexer.MarkdownLexer
import org.intellij.markdown.parser.*
import org.intellij.markdown.parser.constraints.CommonMarkdownConstraints
import org.intellij.markdown.parser.constraints.MarkdownConstraints
import org.intellij.markdown.parser.constraints.getCharsEaten
import org.intellij.markdown.parser.markerblocks.MarkerBlockProvider
import org.intellij.markdown.parser.markerblocks.providers.*
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.SequentialParserManager
import org.intellij.plugins.markdown.lang.parser.blocks.CommentAwareLinkReferenceDefinitionProvider
import java.net.URI

object RMarkdownFlavourDescriptor : CommonMarkFlavourDescriptor() {
  private val myGfmFlavourDescriptor: GFMFlavourDescriptor = GFMFlavourDescriptor()

  override val markerProcessorFactory: MarkerProcessorFactory get() = RMarkdownProcessFactory

  override val sequentialParserManager: SequentialParserManager get() = myGfmFlavourDescriptor.sequentialParserManager

  override fun createHtmlGeneratingProviders(linkMap: LinkMap, baseURI: URI?): Map<IElementType, GeneratingProvider> {
    return myGfmFlavourDescriptor.createHtmlGeneratingProviders(linkMap, baseURI)
  }

  override fun createInlinesLexer(): MarkdownLexer {
    return myGfmFlavourDescriptor.createInlinesLexer()
  }
}

private object RMarkdownProcessFactory : MarkerProcessorFactory {
  override fun createMarkerProcessor(productionHolder: ProductionHolder): MarkerProcessor<*> {
    return RMarkdownMarkerProcessor(productionHolder, CommonMarkdownConstraints.BASE)
  }
}

private class RMarkdownMarkerProcessor(productionHolder: ProductionHolder,
                                       constraints: MarkdownConstraints) :
  CommonMarkMarkerProcessor(productionHolder, constraints) {


  private val markerBlockProviders =
    listOf(
      CodeBlockProvider(),
      HorizontalRuleProvider(),
      RCodeFenceProvider(),
      SetextHeaderProvider(),
      BlockQuoteProvider(),
      ListMarkerProvider(),
      HtmlBlockProvider(),
      GitHubTableMarkerProvider(),
      AtxHeaderProvider(),
      CommentAwareLinkReferenceDefinitionProvider()
    )

  override fun populateConstraintsTokens(pos: LookaheadText.Position,
                                         constraints: MarkdownConstraints,
                                         productionHolder: ProductionHolder) {
    if (constraints !is GFMConstraints || !constraints.hasCheckbox()) {
      super.populateConstraintsTokens(pos, constraints, productionHolder)
      return
    }

    val line = pos.currentLine
    var offset = pos.offsetInCurrentLine
    while (offset < line.length && line[offset] != '[') {
      offset++
    }
    if (offset == line.length) {
      super.populateConstraintsTokens(pos, constraints, productionHolder)
      return
    }

    check(constraints.types.isNotEmpty())
    val type = when (constraints.types.last()) {
      '>' ->
        MarkdownTokenTypes.BLOCK_QUOTE
      '.', ')' ->
        MarkdownTokenTypes.LIST_NUMBER
      else ->
        MarkdownTokenTypes.LIST_BULLET
    }
    val middleOffset = pos.offset - pos.offsetInCurrentLine + offset
    val endOffset = Math.min(pos.offset - pos.offsetInCurrentLine + constraints.getCharsEaten(pos.currentLine),
                             pos.nextLineOrEofOffset)

    productionHolder.addProduction(listOf(
      SequentialParser.Node(pos.offset..middleOffset, type),
      SequentialParser.Node(middleOffset..endOffset, GFMTokenTypes.CHECK_BOX)
    ))
  }

  override fun getMarkerBlockProviders(): List<MarkerBlockProvider<StateInfo>> {
    return markerBlockProviders
  }
}
