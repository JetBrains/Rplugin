package org.jetbrains.r.injections

import org.jetbrains.r.RLanguage
import org.jetbrains.r.RUsefulTestCase
import org.junit.Assert.assertNotEquals

class RInjectionsTest : RUsefulTestCase() {
  fun testInjectionInShinyMarkdownCall() {
    doTest(
      """
        markdown("
        # Markd<caret>own Example
        This is a markdown paragraph
        ")
      """.trimIndent()
    )
  }

  fun testInjectionInNamedParameterOfShinyMarkdownCall() {
    doTest(
      """
        markdown(mds = "
        # Markd<caret>own Example
        This is a markdown paragraph
        ")
      """.trimIndent()
    )
  }

  fun testInjectionInShinyHtmlTemplateCall() {
    doTest("htmlTemplate(text_ = \"<div st<caret>yle='color: red'>Hello</div>\")")
  }

  fun testInjectionInShinyInsertUICall() {
    doTest("insertUI(selector = \"#my_<caret>id\")")
  }

  fun testInjectionInShinyRemoveUICall() {
    doTest("removeUI(selector = \"#my_<caret>id\")")
  }

  private fun doTest(code: String) {
    addLibraries()
    myFixture.configureByText("test.r", code)
    val element = myFixture.file.findElementAt(myFixture.caretOffset)
    assertNotEquals(element!!.language, RLanguage.INSTANCE)
  }
}