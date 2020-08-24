package org.jetbrains.r.resolve

import junit.framework.TestCase
import org.jetbrains.r.RFileType
import org.jetbrains.r.RLightCodeInsightFixtureTestCase

class RShinyResolveTest : RLightCodeInsightFixtureTestCase() {
  fun testInputAttributes() {
    doTest("\"title\"",
           """
             ui <- fluidPage(
               textInput(inputId = "title", 
                 label = "label",
                 value = "value")
             )

             server <- function(input, output) {
               output${'$'}hist <- renderPlot({
                 hist(rnorm(input${'$'}num), main = input${'$'}tit<caret>le)
               })
             }
           """.trimIndent()
    )
  }

  private fun doTest(resolveTargetParentText: String, text: String) {
    myFixture.configureByText(RFileType, text)
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