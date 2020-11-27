package org.jetbrains.r.completion

import org.jetbrains.r.RLightCodeInsightFixtureTestCase

class RMarkdownCompletionTest : RLightCodeInsightFixtureTestCase() {
  fun testIdentifierWithDot() {
    doTest(
      """
        ```{r}
        x.new <- "text"
        qplot(x<caret>)
        ```
      """.trimIndent(),

      '.',

      """
        ```{r}
        x.new <- "text"
        qplot(x.)
        ```
      """.trimIndent(),
    )
  }

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  private fun doTest(text: String, characterToType: Char, expectedResult: String) {
    myFixture.configureByText("a.Rmd", text)
    myFixture.completeBasic()
    myFixture.type(characterToType)
    myFixture.completeBasic()
    myFixture.checkResult(expectedResult)
  }
}