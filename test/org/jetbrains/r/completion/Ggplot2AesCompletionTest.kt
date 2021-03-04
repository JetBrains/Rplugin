package org.jetbrains.r.completion

import junit.framework.TestCase
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class Ggplot2AesCompletionTest : RProcessHandlerBaseTestCase() {
  fun testInGgplot() {
    checkCompletion(
      """
        ggplot(df, aes(x, st<caret>))
      """.trimIndent(),
      listOf("stroke"),
      listOf()
    )
  }

  fun testDoNotCompletePresentedArguments() {
    checkCompletion(
      """
        ggplot(df, aes(x, stroke=5, str<caret>))
      """.trimIndent(),
      listOf(),
      listOf("stroke"),
    )
  }

  fun testInGeomHistogram() {
    checkCompletion(
      """
        ggplot(df, aes(x, y)) + geom_histogram(aes(li<caret>))
      """.trimIndent(),
      listOf("linetype"),
      listOf(),
    )
  }

  fun testInGeomText() {
    checkCompletion(
      """
        ggplot(df, aes(x, y)) + geom_text(aes(hj<caret>))
      """.trimIndent(),
      listOf("hjust"),
      listOf()
    )
  }

  fun testInStatContourFilled() {
    checkCompletion(
      """
        stat_contour_filled(aes(fi<caret>))
      """.trimIndent(),
      listOf("fill"),
      listOf()
    )
  }

  fun testGeomValueCompletionNoStringForQplot() {
    checkCompletion(
      """
        qplot(x = cty, y = hwy, data = mpg, geom = <caret>)
      """.trimIndent(),
      listOf("\"point\"", "\"bar\"", "\"polygon\""),
      listOf()
    )
  }

  fun testGeomValueCompletionStringForQplot() {
    checkCompletion(
      """
        qplot(x = cty, y = hwy, data = mpg, geom = "<caret>")
      """.trimIndent(),
      listOf("point", "bar", "polygon"),
      listOf()
    )
  }

  fun testGeomValueCompletionNoStringForStat() {
    checkCompletion(
      """
        stat_bin(geom = <caret>)
      """.trimIndent(),
      listOf("\"point\"", "\"bar\"", "\"polygon\""),
      listOf()
    )
  }

  fun testGeomValueCompletionStringForStat() {
    checkCompletion(
      """
        stat_bin(geom = "<caret>")
      """.trimIndent(),
      listOf("point", "bar", "polygon"),
      listOf()
    )
  }

  private fun checkCompletion(text: String, expectedToBePresent: List<String>, expectedToBeMissing: List<String>) {
    myFixture.configureByText("a.R", text)
    val result = myFixture.completeBasic()
    TestCase.assertNotNull(result)
    val lookupStrings = result.map { it.lookupString }
    for (completionElement in expectedToBePresent) {
      assertEquals(1, lookupStrings.count { it == completionElement })
    }
    for (completionElement in expectedToBeMissing) {
      assertEquals(0, lookupStrings.count { it == completionElement })
    }
  }
}