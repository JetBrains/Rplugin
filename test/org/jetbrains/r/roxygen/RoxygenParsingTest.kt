/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen

import com.intellij.r.psi.roxygen.parsing.RoxygenParserDefinition
import com.intellij.testFramework.ParsingTestCase
import org.jetbrains.r.RUsefulTestCase

class RoxygenParsingTest : ParsingTestCase("roxygen/parser", "roxygen", RoxygenParserDefinition()) {
  fun testSingleTextLine() = doTest(true)

  fun testAllLinksAndTextTogether() = doTest(true)

  fun testAutolink() = doTest(true)

  fun testAutolinkBadURI() = doTest(true)

  fun testAutolinkIntoAutolink() = doTest(true)

  fun testAutolinkIntoHelpPageLinkText() = doTest(true)

  fun testAutolinkIntoLinkDestinationText() = doTest(true)

  fun testDoubleEscapedHelpPageLink() = doTest(true)

  fun testEscapedAutolinkIntoLinkDestinationText() = doTest(true)

  fun testEscapedHelpPageLink() = doTest(true)

  fun testHelpPageLink() = doTest(true)

  fun testHelpPageLinkBadIdentifier() = doTest(true)

  fun testHelpPageLinkIdentifier() = doTest(true)

  fun testHelpPageLinkIntoHelpPageLink() = doTest(true)

  fun testHelpPageLinkOpenBracketBefore() = doTest(true)

  fun testHelpPageLinkWithText() = doTest(true)

  fun testHelpPageLinkWithTextAndTextBeforeAfter() = doTest(true)

  fun testLessGreaterIsNotAutolink() = doTest(true)

  fun testLinkDestination() = doTest(true)

  fun testLinkDestinationExtraWhitespace() = doTest(true)

  fun testLinkDestinationParenthesisInside() = doTest(true)

  fun testLinkDestinationUnmatchedParenthesisInside() = doTest(true)

  fun testMultiLineWithIndents() = doTest(true)

  fun testMultiTextLine() = doTest(true)

  fun testParamTag() = doTest(true)

  fun testParamTagExtraWhitespace() = doTest(true)

  fun testSingleEmptyLine() = doTest(true)

  fun testTags() = doTest(true)

  fun testTagsIntoExample() = doTest(true)

  fun testTwoHelpPageLinkInARow() = doTest(true)

  fun testUnclosedAutolink() = doTest(true)

  fun testUnclosedHelpPageLink() = doTest(true)

  fun testUnclosedLinkDestination() = doTest(true)

  override fun getTestDataPath(): String = RUsefulTestCase.TEST_DATA_PATH
}