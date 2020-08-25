package org.jetbrains.r.completion

import junit.framework.TestCase
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class RShinyCompletionTest : RProcessHandlerBaseTestCase() {
  fun testInputCompletion() {
    checkCompletion(
      """
      ui <- fluidPage(
        actionButton(inputId = "norm", label = "Normal"),
        plotOutput("hist")
      )

      server <- function(input, output) {
        rv <- reactiveValues(data = rnorm(100))
        observeEvent(input${"$"}<caret> , { rv${"$"}data <- rnorm(100) })
        output${"$"}hist <- renderPlot({
        })
      }
      """.trimIndent(),
      listOf("norm")
    )
  }

  fun testOutputCompletion() {
    checkCompletion(
      """
      ui <- fluidPage(
        actionButton(inputId = "norm", label = "Normal"),
        plotOutput(outputId = "hist")
      )

      server <- function(input, output) {
        rv <- reactiveValues(data = rnorm(100))
        observeEvent(input${"$"}norm , { rv${"$"}data <- rnorm(100) })
        output${"$"}<caret> <- renderPlot({
        })
      }
      """.trimIndent(),
      listOf("hist")
    )
  }

  private fun checkCompletion(text: String, expectedToBePresent: List<String>) {
    myFixture.configureByText("a.R", text)
    val result = myFixture.completeBasic()
    TestCase.assertNotNull(result)
    val lookupStrings = result.map { it.lookupString }
    for (completionElement in expectedToBePresent) {
      assertEquals(1, lookupStrings.count { it == completionElement })
    }
  }
}