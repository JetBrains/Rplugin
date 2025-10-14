package org.jetbrains.r.injections

import com.intellij.lang.Language
import com.intellij.r.psi.RLanguage
import org.jetbrains.r.RUsefulTestCase
import org.junit.Assert.assertNotEquals

class RInjectionsTest : RUsefulTestCase() {
  fun testInjectionInShinyMarkdownCall() {
    if (Language.findLanguageByID("Markdown") == null) {
      //Language Markdown not available
      return
    }

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
    if (Language.findLanguageByID("Markdown") == null) {
      //Language Markdown not available
      return
    }

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
    if (Language.findLanguageByID("HTML") == null) {
      //Language HTML not available
      return
    }

    doTest("htmlTemplate(text_ = \"<div st<caret>yle='color: red'>Hello</div>\")")
  }

  fun testInjectionInShinyInsertUICall() {
    if (Language.findLanguageByID("CSS") == null) {
      //Language CSS not available
      return
    }

    doTest("insertUI(selector = \"#my_<caret>id\")")
  }

  fun testInjectionInShinyRemoveUICall() {
    if (Language.findLanguageByID("CSS") == null) {
      //Language CSS not available
      return
    }

    doTest("removeUI(selector = \"#my_<caret>id\")")
  }

  fun testInjectionInShinyTagMembers() {
    if (Language.findLanguageByID("CSS") == null) {
      //Language CSS not available
      return
    }

    doTest("tags${"$"}input(style=\"co<caret>lor:red\")")
  }

  fun testInjectionInShinyTags() {
    if (Language.findLanguageByID("CSS") == null) {
      //Language CSS not available
      return
    }
    doTest("h1(style=\"co<caret>lor:red\")")
  }

  private fun doTest(code: String) {
    addLibraries()
    myFixture.configureByText("test.r", code)
    val element = myFixture.file.findElementAt(myFixture.caretOffset)
    assertNotEquals(element!!.language, RLanguage.INSTANCE)
  }
}