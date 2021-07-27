package org.jetbrains.r.completion.mlcompletion

import com.intellij.codeInsight.lookup.LookupElement
import junit.framework.TestCase
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionHttpResponse
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionLocalServerService
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionUtils.isRMachineLearningLookupElement
import org.jetbrains.r.mock.MockMachineLearningCompletionServer
import org.jetbrains.r.run.RProcessHandlerBaseTestCase
import org.jetbrains.r.settings.MachineLearningCompletionSettings
import java.util.function.Function
import java.util.stream.Collectors


class RMachineLearningCompletionTest : RProcessHandlerBaseTestCase() {

  private fun setMachineLearningResponse(vararg variants: String, delay: Int = 0) {
    val mlServer = MachineLearningCompletionLocalServerService.getInstance() as MockMachineLearningCompletionServer
    val variantsWithProbabilities = List(variants.size) {
      val probability = 1.0 - it * (1.0 / variants.size)
      MachineLearningCompletionHttpResponse.CompletionVariant(variants[it], probability)
    }
    mlServer.returnOnNextCompletion(MachineLearningCompletionHttpResponse(variantsWithProbabilities), delay)
  }

  private class TestCompletionResult(val elements: Array<out LookupElement>, val lookupStrings: List<String>) {
    fun andDoesntContain(vararg notExpected: String) {
      assertDoesntContain(lookupStrings, notExpected)
    }
  }

  private fun complete(text: String,
                       mlVariants: Array<String>,
                       delay: Int = 0): TestCompletionResult {
    setMachineLearningResponse(*mlVariants, delay = delay)
    myFixture.configureByText("foo.R", text)
    val result = myFixture.completeBasic()
    val lookupStrings = result.map { it.lookupString }
    return TestCompletionResult(result, lookupStrings)
  }

  private fun containsOrderedTest(text: String,
                                  mlVariants: Array<String>,
                                  vararg expected: String,
                                  delay: Int = 0): TestCompletionResult =
    complete(text, mlVariants, delay).also { result ->
      assertContainsOrdered(result.lookupStrings, *expected)
    }

  private fun duplicateTest(text: String,
                            mlVariants: Array<String>,
                            vararg expected: String,
                            delay: Int = 0): TestCompletionResult =
    containsOrderedTest(text, mlVariants, *expected, delay = delay).also { result ->
      assertContainsOnce(result.lookupStrings, *expected)
    }

  private fun assertContainsOnce(lookupStrings: List<String>, vararg expected: String) {
    val counts = lookupStrings.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
    for (lookupString in expected) {
      TestCase.assertTrue(counts[lookupString] == 1L)
    }
  }

  fun testMLCompletionIsWorking() {
    containsOrderedTest("""
      hello_world <- 123456
      hello_<caret>
    """.trimIndent(), mlVariants = arrayOf("hello_x", "hello_y"), "hello_x", "hello_y", "hello_world")

    containsOrderedTest("""
      long.variable.name <- 11
      longer.variable.name <- 12
      lon<caret>
    """.trimIndent(), mlVariants = arrayOf("longest.variable.name"),
                        "longest.variable.name", "long.variable.name", "longer.variable.name")
  }

  fun testDuplicatesAreFiltered() {
    duplicateTest("""
      buzz <- 5
      fizz <- 3
      fizzbuzz <- 16
      <caret>
    """.trimIndent(), mlVariants = arrayOf("buzz"), "buzz", "fizz", "fizzbuzz"
    )
  }

  fun testRElementsAreLeftWhenDuplicatesPresent() {
    duplicateTest("""
      identity <- function (x) x
      omega <- function (x) x(x)
      <caret>
    """.trimIndent(), mlVariants = arrayOf("identity", "omega"), "identity", "omega"
    ).also { result ->
      for (lookupElement in result.elements) {
        TestCase.assertTrue(!lookupElement.isRMachineLearningLookupElement())
      }
    }
  }

  fun testMLCompletionReordersElements() {
    val text = """
      alpha <- 1
      beta <- 2
      delta <- 4
      <caret>
    """.trimIndent()

    containsOrderedTest(text, mlVariants = arrayOf(), "alpha", "beta", "delta")
    val mlVariants = arrayOf("beta", "delta", "alpha")
    duplicateTest(text, mlVariants = mlVariants, *mlVariants)

    val mlVariantsWithNewElements = arrayOf("xi", "alpha", "delta", "beta")
    duplicateTest(text, mlVariants = mlVariantsWithNewElements, *mlVariantsWithNewElements)
  }

  fun testMLCompletionObeysPrefixMatching() {
    val text = """
      hello_world <- 1
      hello_everyone <- 2
      hello_<caret>
    """.trimIndent()
    containsOrderedTest(text, mlVariants = arrayOf("alpha", "beta"), "hello_everyone", "hello_world")
      .andDoesntContain("alpha", "beta")
    containsOrderedTest(text, mlVariants = arrayOf("alpha", "beta", "hello_world"), "hello_world", "hello_everyone")
      .andDoesntContain("alpha", "beta")
  }

  fun testMLCompletionTimeoutIsWorking() {
    val timeout = 2 * MachineLearningCompletionSettings.DEFAULT_REQUEST_TIMEOUT_MS
    containsOrderedTest("""
      alpha <- 1
      beta <- 2
      <caret>
    """.trimIndent(), mlVariants = arrayOf("gamma", "delta"), "alpha", "beta", delay = timeout
    ).andDoesntContain("gamma", "delta")
  }
}
