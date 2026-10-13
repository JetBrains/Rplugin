/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen

import com.intellij.r.psi.roxygen.parsing.RoxygenParserDefinition
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.parsingFixture
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import org.jetbrains.r.RUsefulTestCase
import org.junit.jupiter.api.Test

@TestApplication
class RoxygenParsingTest {
  companion object {
    private val projectFixture = projectFixture()
  }

  private val parsing by parsingFixture(
    projectFixture,
    RUsefulTestCase.TEST_DATA_PATH + "/roxygen/parser",
    "roxygen",
    definitions = arrayOf(RoxygenParserDefinition()),
  )

  @Test
  fun testSingleTextLine() = parsing.doTest(true)

  @Test
  fun testAllLinksAndTextTogether() = parsing.doTest(true)

  @Test
  fun testAutolink() = parsing.doTest(true)

  @Test
  fun testAutolinkBadURI() = parsing.doTest(true)

  @Test
  fun testAutolinkIntoAutolink() = parsing.doTest(true)

  @Test
  fun testAutolinkIntoHelpPageLinkText() = parsing.doTest(true)

  @Test
  fun testAutolinkIntoLinkDestinationText() = parsing.doTest(true)

  @Test
  fun testDoubleEscapedHelpPageLink() = parsing.doTest(true)

  @Test
  fun testEscapedAutolinkIntoLinkDestinationText() = parsing.doTest(true)

  @Test
  fun testEscapedHelpPageLink() = parsing.doTest(true)

  @Test
  fun testHelpPageLink() = parsing.doTest(true)

  @Test
  fun testHelpPageLinkBadIdentifier() = parsing.doTest(true)

  @Test
  fun testHelpPageLinkIdentifier() = parsing.doTest(true)

  @Test
  fun testHelpPageLinkIntoHelpPageLink() = parsing.doTest(true)

  @Test
  fun testHelpPageLinkOpenBracketBefore() = parsing.doTest(true)

  @Test
  fun testHelpPageLinkWithText() = parsing.doTest(true)

  @Test
  fun testHelpPageLinkWithTextAndTextBeforeAfter() = parsing.doTest(true)

  @Test
  fun testLessGreaterIsNotAutolink() = parsing.doTest(true)

  @Test
  fun testLinkDestination() = parsing.doTest(true)

  @Test
  fun testLinkDestinationExtraWhitespace() = parsing.doTest(true)

  @Test
  fun testLinkDestinationParenthesisInside() = parsing.doTest(true)

  @Test
  fun testLinkDestinationUnmatchedParenthesisInside() = parsing.doTest(true)

  @Test
  fun testMultiLineWithIndents() = parsing.doTest(true)

  @Test
  fun testMultiTextLine() = parsing.doTest(true)

  @Test
  fun testParamTag() = parsing.doTest(true)

  @Test
  fun testParamTagExtraWhitespace() = parsing.doTest(true)

  @Test
  fun testSingleEmptyLine() = parsing.doTest(true)

  @Test
  fun testTags() = parsing.doTest(true)

  @Test
  fun testTagsIntoExample() = parsing.doTest(true)

  @Test
  fun testTwoHelpPageLinkInARow() = parsing.doTest(true)

  @Test
  fun testUnclosedAutolink() = parsing.doTest(true)

  @Test
  fun testUnclosedHelpPageLink() = parsing.doTest(true)

  @Test
  fun testUnclosedLinkDestination() = parsing.doTest(true)
}