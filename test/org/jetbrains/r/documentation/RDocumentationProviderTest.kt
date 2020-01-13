/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.documentation

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.documentation.DocumentationManager.ORIGINAL_ELEMENT_KEY
import com.intellij.openapi.application.PathManager
import com.intellij.psi.PsiManager
import org.jetbrains.r.console.RConsoleRuntimeInfoImpl
import org.jetbrains.r.console.addRuntimeInfo
import org.jetbrains.r.run.RProcessHandlerBaseTestCase
import java.io.File
import java.io.IOException

class RDocumentationProviderTest : RProcessHandlerBaseTestCase() {

  private val docProvider = RDocumentationProvider()
  private val pathToDocumentation = "${PathManager.getSystemPath()}${File.separator}documentation"

  override fun getTestDataPath() = super.getTestDataPath() + "/documentation/" + javaClass.simpleName.replace("Test", "")

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  override fun tearDown() {
    if (!File(pathToDocumentation).deleteRecursively()) {
      throw IOException("Can't delete documentation dir")
    }
    super.tearDown()
  }

  fun testLogicalKeywords() {
    val testHelper = makeHelper("logical", "base")
    val logicKeywords = listOf("TRUE", "FALSE", "T", "F")
    for (keyword in logicKeywords) {
      testHelper(keyword)
    }
  }

  fun testNaKeywords() {
    val testHelper = makeHelper("NA", "base")
    val naKeywords = listOf("NA", "NA_integer_", "NA_real_", "NA_complex_", "NA_character_")
    for (keyword in naKeywords) {
      testHelper(keyword)
    }
  }

  fun testIsFiniteKeywords() {
    val testHelper = makeHelper("is.finite", "base")
    val finiteKeywords = listOf("Inf", "NaN")
    for (keyword in finiteKeywords) {
      testHelper(keyword)
    }
  }

  fun testControlKeywords() {
    val testHelper = makeHelper("Control", "base")
    val controlKeywords = listOf("if", "else", "repeat", "while", "for", "in", "next", "break")
    for (keyword in controlKeywords) {
      testHelper(keyword)
    }
  }

  fun testFunctionKeywords() {
    val testHelper = makeHelper("function", "base")
    val functionKeywords = listOf("function", "return")
    for (keyword in functionKeywords) {
      testHelper(keyword)
    }
  }

  fun testNull() {
    doTest("NULL", "NULL", "base")
  }

  fun testArithmeticOperators() {
    val testHelper = makeHelper("Arithmetic", "base")
    val arithmeticOperators = listOf("+", "-", "*", "/", "**", "^", "%%", "%/%")
    for (operator in arithmeticOperators) {
      testHelper("2 <caret>${operator} 3")
      testHelper("2<caret>${operator}3")
    }
  }

  fun testRelationalOperators() {
    val testHelper = makeHelper("Comparison", "base")
    val arithmeticOperators = listOf("==", ">=", "<=", "!=")
    for (operator in arithmeticOperators) {
      testHelper("2 <caret>${operator} 3")
      testHelper("2<caret>${operator}3")
    }
  }

  fun testLogicalOperators() {
    val testHelper = makeHelper("Logic", "base")
    val arithmeticOperators = listOf("|", "||", "&", "&&")
    testHelper("!x")
    for (operator in arithmeticOperators) {
      testHelper("2 <caret>${operator} 3")
      testHelper("2<caret>${operator}3")
    }
  }

  fun testTilda() {
    doTest("~x", "tilde", "base")
  }

  fun testAssignOperators() {
    val testHelper = makeHelper("assignOps", "base")
    val leftOperators = listOf("<<-", "<-", "=")
    val rightOperators = listOf("->>", "->")

    for (operator in leftOperators) {
      testHelper("a<caret>${operator}5")
      testHelper("a <caret>${operator} 5")
    }

    for (operator in rightOperators) {
      testHelper("5<caret>${operator}a")
      testHelper("5 <caret>${operator} a")
    }
  }

  fun testNotStandardLibrary() {
    loadLibraries("dplyr", "crayon")
    doTest("library(dplyr); <caret>dplyr::group_by(iris)", "group_by", "dplyr")
    doTest("library(crayon); concat(\"foo\" <caret>%+% \"bar\")", "concat", "crayon")
    doTest("library(crayon); b<caret>lue(\"Hello\", \"world!\")", "crayon", "crayon")
  }

  fun testQualifiedName() {
    doTest("base::print(0)", "print", "base")
    doTest("base<caret>::print(0)", "print", "base")
    doTest("base::<caret>print(0)", "print", "base")

    loadLibraries("dplyr", "tibble")
    doTest("library(dplyr); dplyr<caret>::data_frame()", "reexports", "dplyr")
    doTest("library(tibble); tibble<caret>::data_frame()", "deprecated", "tibble")
    doTest("library(dplyr); dplyr::<caret>data_frame()", "reexports", "dplyr")
    doTest("library(tibble); tibble::<caret>data_frame()", "deprecated", "tibble")
  }

  fun testLocalFunction() {
    val accessingOperators = listOf("::", ":::")
    for (operator in accessingOperators) {
      doTest("""
        #' MyDocumentation
        #' @param x A number
        #' @param y A number
        help <- function(x, y) {
          x + y
        }
        
        help(3, 4)
        utils${operator}<caret>help("help")
        """.trimIndent(), "help", "utils")

      doTest("""
        #' MyDocumentation
        #' @param x A number
        #' @param y A number
        help <- function(x, y) {
          x + y
        }
        
        he<caret>lp(3, 4)
        utils${operator}help("help")
        """.trimIndent(), "help", "test.R")
    }
  }

  fun testColon() {
    doTest("for (i in 1<caret>:3) {}", "Colon", "base")
  }

  fun testHelp() {
    val testHelper = makeHelper("help", "utils")
    testHelper("?\"print\"")
    testHelper("help(\"print\")")
  }

  fun testLibrary() {
    doTest("library(tools)", "library", "base")
    val libraries = listOf("base", "datasets", "graphics", "stats", "utils")
    for (library in libraries) {
      doTest("library(<caret>$library)", "$library-package", library)
    }
  }

  fun testCaretBeforeBrackets() {
    doTest("print<caret>(1)", "print", "base")
    doTest("packageDescription<caret>(\"dplyr\")", "packageDescription", "utils")
    doTest("20 / euro<caret>[\"ATS\"]", "euro", "datasets")
  }

  fun testDotsInFunctionName() {
    doTest(".rowSums(matrix(c(1, 1, 2, 2), 2, 2), 2, 2)", "colSums", "base")
    doTest(".Last.value", "Last.value", "base")
  }

  fun testNavigateLink() {
    myFixture.configureByText("foo.R", "x <- 123")
    doLinkTest("base/html/print.html", "page for print")
    doLinkTest("base/html/00Index.html", "The R Base Package")
  }

  fun testExternalLink() {
    myFixture.configureByText("foo.R", "x <- 123")
    doLinkTest("https://www.loc.gov/marc/relators/relaterm.html", "MARC Code List for Relators")
    doLinkTest("https://CRAN.R-project.org/package=Matrix", "Matrix: Sparse and Dense Matrix Classes and Methods")
  }

  fun testNavigateLinkDoesntWork() {
    myFixture.configureByText("foo.txt", "x <- 123")
    testLinkNull("base/html/print.html")
    testLinkNull("base/html/00Index.html")
  }

  fun testExternalLinkDoesntWork() {
    myFixture.configureByText("foo.txt", "x <- 123")
    testLinkNull("https://www.loc.gov/marc/relators/relaterm.html")
    testLinkNull("https://CRAN.R-project.org/package=Matrix")
  }

  // ---- END OF TESTS ---

  private fun makeHelper(page: String, pack: String): (String) -> Unit {
    return { doTest(it, page, pack) }
  }

  private fun doTest(text: String, page: String, pack: String) {

    myFixture.configureByText("test.R", text)
    val docElement = DocumentationManager.getInstance(myFixture.project).findTargetElement(myFixture.editor, myFixture.file)
    val originalPointer = docElement.getUserData(ORIGINAL_ELEMENT_KEY)
    val originalElement = if (originalPointer != null) originalPointer.getElement() else null
    assertNotNull("No original element at ${psiUnderCaret()?.text}", originalElement)
    originalElement!!.containingFile.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    docElement!!.containingFile.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    val docText = docProvider.generateDoc(docElement, originalElement)

    assertNotNull("No document found for ${originalElement.text}", docText)
    assertTrue("Wrong help page returned for ${originalElement.text}: $docText", docText!!.contains("page for $page")
                                                                                 && docText.contains("Package <em>$pack</em>"))
    assertSame("Bad links in doc for ${originalElement.text}",
               Regex("href=\"psi_element://(?!http)").findAll(docText).count(), Regex("\\.html").findAll(docText).count())
  }

  private fun testLinkNull(link: String) {
    val psiManager = PsiManager.getInstance(project)
    val docElement = docProvider.getDocumentationElementForLink(psiManager, link, myFixture.file)
    assertNull("doc element should be null for other languages", docElement)
    val docElementNullContext = docProvider.getDocumentationElementForLink(psiManager, link, null)
    assertNull("doc element should be null for other languages", docElementNullContext)
  }

  private fun doLinkTest(link: String, substr: String) {
    val psiManager = PsiManager.getInstance(project)
    val docElement = docProvider.getDocumentationElementForLink(psiManager, link, myFixture.file)
    assertTrue("Bad element fo link ${link}", docElement != null)

    docElement!!.containingFile.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    val docText = docProvider.generateDoc(docElement, null)

    assertNotNull("No document found for ${link}", docText)
    assertTrue("Wrong help page returned for ${link}: $docText", docText!!.contains(substr))
  }

  private fun loadLibraries(vararg libraries: String) {
    for (library in libraries) {
      rInterop.executeCode("library($library)", false)
    }
  }

  private fun psiUnderCaret() = myFixture.file.findElementAt(myFixture.editor.caretModel.offset)
}
