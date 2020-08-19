package org.jetbrains.r.resolve

import com.intellij.openapi.fileTypes.LanguageFileType
import junit.framework.TestCase
import org.jetbrains.r.RFileType
import org.jetbrains.r.console.RConsoleRuntimeInfoImpl
import org.jetbrains.r.console.addRuntimeInfo
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class RTableColumnResolveTest : RProcessHandlerBaseTestCase() {
  fun testResolveInCall() {
    doTest(
      "my_column=letters",

      """
        tbl <- dplyr::tibble(my_column=letters)
        tbl %>% dplyr::filter(my_colu<caret>mn)
      """.trimIndent())
  }

  fun testResolveToCountWithoutName() {
    doTest(
      "dplyr::count(my_column, n)",

      """
        tbl <- dplyr::tibble(my_column = letters, n = letters) %>% dplyr::count(my_column, n)
        tbl %>% dplyr::filter(n<caret>n)
      """.trimIndent())
  }

  fun testResolveToCountWithName() {
    doTest(
      "name = 'CountColumn'",

      """
        tbl <- dplyr::tibble(my_column=letters) %>% dplyr::count(my_column, name = 'CountColumn')
        tbl %>% dplyr::filter(Count<caret>Column == 1)
      """.trimIndent())
  }

  fun testResolveInFunction() {
    doTest(
      "my_column = letters",

      """
        f <- function() {
         tbl <- dplyr::tibble(my_column = letters)
         tbl %>% dplyr::filter(my<caret>_column)
        }        
      """.trimIndent()
    )
  }

  fun testFunctionWithReferenceToGlobalVariable() {
    doTest(
      "my_column = letters",

      """
        tbl <- dplyr::tibble(my_column = letters)
        f <- function() {
         tbl %>% dplyr::filter(my_colu<caret>mn)
        }
      """.trimIndent()
    )
  }

  fun testChainedAssignment() {
    doTest(
      "another_column = letters",

      """
        tbl <- dplyr::tibble(my_column = letters, another_column = letters)
        tbl <- tbl %>% dplyr::mutate(third_column = letters)
        tbl %>% dplyr::filter(another_co<caret>lumn)
      """.trimIndent()
    )
  }

  fun testColumnDefinedViaDplyrMutate() {
    doTest(
      "third_column = letters",

      """
        tbl <- smthing %>% dplyr::mutate(third_column = letters)
        tbl %>% dplyr::filter(third_<caret>column)
      """.trimIndent()
    )
  }

  fun testResolveInNestedFunctions() {
    doTest(
      "yyyy_ac = 1 + 2",

      """
        transmute(mutate(table, yyyy_ac = 1 + 2), yyyy_ax = 1, yyyy_ay = yyyy<caret>_ac, yyyy_az = 2)
      """.trimIndent()
    )
  }

  private fun doTest(resolveTargetParentText: String, text: String, fileType: LanguageFileType? = RFileType) {
    fileType?.let { myFixture.configureByText(it, text) }
    myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    val results = resolve()
    if (resolveTargetParentText.isBlank()) {
      TestCase.assertEquals(results.size, 0)
      return
    }
    TestCase.assertEquals(results.size, 1)
    val element = results[0].element!!
    TestCase.assertTrue(element.isValid)
    TestCase.assertEquals(resolveTargetParentText, element.text)
  }
}