package org.jetbrains.r.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.editor.impl.EditorImpl
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.run.configuration.RRunConfiguration
import org.jetbrains.r.run.configuration.RRunConfigurationProducer
import org.jetbrains.r.settings.RSettings

class RRunConfigurationProducerTest : RUsefulTestCase() {
  fun testCreateRunConfiguration() {
    myFixture.configureByText(
      "my_file.R",
      """
        a <- c(1, 2, 3)
      """.trimIndent()
    )

    val runConfiguration = getConfigurationFromCaret()
    assertNotNull(runConfiguration)

    if (runConfiguration !is RRunConfiguration) {
      fail("RRunConfiguration expected")
    }
    assertEquals("/src", (runConfiguration as RRunConfiguration).workingDirectory)
    assertEquals("/src/my_file.R", runConfiguration.filePath)
    assertEquals("", runConfiguration.scriptArguments)
    assertEquals(0, runConfiguration.environmentVariablesData.envs.size)
  }

  fun testDoNotCreateRunConfigurationForRMarkdown() {
    myFixture.configureByText(
      "my_file.Rmd",
      """
        ```{r i<caret>nclude = FALSE}
        ```
      """.trimIndent()
    )

    val runConfiguration = getConfigurationFromCaret()
    assertNull(runConfiguration)
  }

  fun testNoRunConfigurationFromEditor() {
    myFixture.configureByText(
      "my_file.R",
      """
        a <- c(1, 2, 3)
      """.trimIndent()
    )

    val editorComponent = EditorComponentImpl(myFixture.editor as EditorImpl)
    val element = myFixture.file.findElementAt(myFixture.caretOffset)
    val configurationContext = object : ConfigurationContext(element!!) {
      override fun getDataContext(): DataContext {
        return SimpleDataContext.getSimpleContext(PlatformCoreDataKeys.CONTEXT_COMPONENT, editorComponent, super.getDataContext())
      }
    }
    val runConfigurationFromContext = RRunConfigurationProducer().createConfigurationFromContext(configurationContext)
    val runConfiguration = runConfigurationFromContext?.configurationSettings?.configuration
    assertNull(runConfiguration)
  }

  override fun setUp() {
    super.setUp()
    val rInterpreter = RInterpreterManager.getInterpreterBlocking(project, RInterpreterUtil.DEFAULT_TIMEOUT)!!
    RSettings.getInstance(myFixture.project).interpreterLocation = rInterpreter.interpreterLocation
  }

  private fun getConfigurationFromCaret(): RunConfiguration? {
    val element = myFixture.file.findElementAt(myFixture.caretOffset)
    val templateConfigurationContext = ConfigurationContext(element!!)

    val configurationContext = RRunConfigurationProducer().createConfigurationFromContext(templateConfigurationContext)
    return configurationContext?.configurationSettings?.configuration
  }
}