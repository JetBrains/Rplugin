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

  fun testOutputAttributes() {
    doTest("\"hist\"",
           """
             ui <- fluidPage(
               textInput(inputId = "title", 
                 label = "label",
                 value = "value"),
               plotOutput(outputId = "hist")
             )

             server <- function(input, output) {
               output${'$'}hi<caret>st <- renderPlot({
                 hist(rnorm(input${'$'}num), main = input${'$'}title)
               })
             }
           """.trimIndent()
    )
  }

  private fun doTest(elementDefinitionText: String, text: String) {
    myFixture.configureByText(RFileType, text)
    val results = resolve()
    if (elementDefinitionText.isBlank()) {
      TestCase.assertEquals(results.size, 0)
      return
    }
    TestCase.assertEquals(results.size, 1)
    val element = results[0].element!!
    TestCase.assertTrue(element.isValid)
    TestCase.assertEquals(elementDefinitionText, element.text)
  }
}