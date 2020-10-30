package org.jetbrains.r.resolve

import junit.framework.TestCase
import org.jetbrains.r.RFileType
import org.jetbrains.r.console.RConsoleRuntimeInfoImpl
import org.jetbrains.r.console.addRuntimeInfo
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class GgplotResolveTest : RProcessHandlerBaseTestCase() {
  fun testResolveFromAes() {
    doTest("sprarg = cyl",
           """
             my_table <- mpg %>% dplyr::mutate(sprarg = cyl) %>% dplyr::mutate(abcd = manufacturer)
             ggplot(data = my_table) + geom_point(mapping = aes(x = spra<caret>rg, y = hwy)) + facet_grid(sprarg)
           """.trimIndent()
    )
  }

  fun testResolveFromFacetGrid() {
    doTest("abcd = manufacturer",
           """
             my_table <- mpg %>% dplyr::mutate(sprarg = cyl) %>% dplyr::mutate(abcd = manufacturer)
             ggplot(data = my_table) + geom_point(mapping = aes(x = sprarg, y = hwy)) + facet_grid(ab<caret>cd)
           """.trimIndent()
    )
  }

  fun testResolveFromQplot() {
    doTest("col1 = norm(10)",
           """
             my_table <- tibble(col1 = norm(10), column2 = norm(10))
             qplot(x = col<caret>1, y = column2, data = my_table, geom = "point")
           """.trimIndent()
    )
  }

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  private fun doTest(elementDefinitionText: String, text: String) {
    myFixture.configureByText(RFileType, text)
    myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))

    val results = resolve()
    TestCase.assertEquals(results.size, 1)
    val element = results[0].element!!
    TestCase.assertTrue(element.isValid)
    TestCase.assertEquals(elementDefinitionText, element.text)
  }
}