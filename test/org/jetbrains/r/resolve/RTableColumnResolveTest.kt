package org.jetbrains.r.resolve

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.SystemInfo.isLinux
import junit.framework.TestCase
import org.jetbrains.r.RFileType
import org.jetbrains.r.console.RConsoleRuntimeInfoImpl
import org.jetbrains.r.console.addRuntimeInfo
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class RTableColumnResolveTest : RProcessHandlerBaseTestCase() {
  override fun setUp() {
    super.setUp()

    addLibraries()

    rInterop.executeCode("library(data.table)", true)
    rInterop.executeCode("library(dplyr)", true)
  }

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

  fun testDplyrSelect() {
    doTest(
      "height",

      """
        a1 <- select(starwars, name, height, mass)
        a2 <- mutate(a1, abc = 2 * he<caret>ight)
      """.trimIndent()
    )
  }

  fun testDplyrJoin() {
    doTest(
      "column_two = letters",

      """
        tbl1 <- dplyr::tibble(letter = letters, column_one = letters)
        tbl2 <- dplyr::tibble(letter = letters, column_two = letters)
        
        tbl1 %>% dplyr::inner_join(tbl2) %>% dplyr::filter(col<caret>umn_two)
      """.trimIndent()
    )
  }

  fun testDplyrRelocate() {
    if (!isLinux) {
      return;
    }
    doTest(
      "column_one = letters",

      """
      tbl1 <- dplyr::tibble(letter = letters, column_one = letters)
      tbl1 %>% dplyr::relocate(column_one) %>% dplyr::filter(column<caret>_one)
      """.trimIndent()
    )
  }

  fun testDplyrRowwise() {
    doTest(
      "column_one = letters",

      """
      tbl1 <- dplyr::tibble(letter = letters, column_one = letters)
      tbl1 %>% dplyr::rowwise() %>% dplyr::filter(column<caret>_one)
      """.trimIndent()
    )
  }

  fun testDplyrSelectEverything() {
    doTest(
      "column_one = letters",

      """
      tbl1 <- dplyr::tibble(letter = letters, column_one = letters)
      tbl1 %>% dplyr::select(everything()) %>% dplyr::filter(column<caret>_one)
      """.trimIndent()
    )
  }

  fun testDataTableSubscription() {
    doTest(
      "\"my_column1\"",

      """
      df <- data.table(my_column1 = c(1, 2), my_column2 = c(3, 4))
      df <- df[, c("my_column1", "my_column2")]
      df[my_colu<caret>mn1]
      """.trimIndent()
    )
  }

  fun testResolveIntoMutateWithSecondTable() {
    doTest(
      "column2 = rnorm(100, 90, 40)",

      """
        car_data <- dplyr::tibble(cyl = rnorm(100), speed = rnorm(100, 90, 40))
        new_tbl <- dplyr::tibble(column1 = rnorm(100), column2 = rnorm(100, 90, 40))
  
        car_data %>% dplyr::mutate(new_tbl) %>% dplyr::filter(co<caret>lumn2)
      """.trimIndent()
    )
  }


  fun testResolveInDataTableSubscription() {
    if (!isLinux) {
      return;
    }
    doTest(
      "my_column_d := 1 + 2",

      """
        dt <- dt[, my_column_d := 1 + 2, my_column_d]
        dt[,.(x=sum(my_col<caret>umn_d),y=sum(my_column_b))]
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