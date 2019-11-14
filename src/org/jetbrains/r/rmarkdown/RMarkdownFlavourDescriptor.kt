/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.flavours.commonmark.CommonMarkMarkerProcessor
import org.intellij.markdown.parser.MarkerProcessor
import org.intellij.markdown.parser.MarkerProcessorFactory
import org.intellij.markdown.parser.ProductionHolder
import org.intellij.markdown.parser.constraints.MarkdownConstraints
import org.intellij.markdown.parser.markerblocks.MarkerBlockProvider
import org.intellij.markdown.parser.markerblocks.providers.*

object RMarkdownFlavourDescriptor : CommonMarkFlavourDescriptor() {
  override val markerProcessorFactory: MarkerProcessorFactory
    get() = RMarkdownProcessFactory
}

private object RMarkdownProcessFactory : MarkerProcessorFactory {
  override fun createMarkerProcessor(productionHolder: ProductionHolder): MarkerProcessor<*> {
    return RMarkdownMarkerProcessor(productionHolder, MarkdownConstraints.BASE)
  }
}

private class RMarkdownMarkerProcessor(productionHolder: ProductionHolder, constraints: MarkdownConstraints) :
  CommonMarkMarkerProcessor(productionHolder, constraints) {

  private val providers = listOf(
    CodeBlockProvider(),
    HorizontalRuleProvider(),
    RCodeFenceProvider(),
    SetextHeaderProvider(),
    BlockQuoteProvider(),
    ListMarkerProvider(),
    AtxHeaderProvider(true),
    HtmlBlockProvider(),
    LinkReferenceDefinitionProvider()
  )

  override fun getMarkerBlockProviders(): List<MarkerBlockProvider<StateInfo>> = providers
}
