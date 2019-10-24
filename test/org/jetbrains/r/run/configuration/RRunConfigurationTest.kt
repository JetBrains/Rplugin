// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.testFramework.PlatformTestCase
import junit.framework.TestCase
import org.jdom.Element
import org.jdom.input.SAXBuilder
import org.jdom.output.XMLOutputter
import org.mockito.Mockito.mock
import java.io.StringReader

class RRunConfigurationTest : PlatformTestCase() {
  fun testReadExternal() {
    val runConfiguration = RRunConfiguration(project, CONFIGURATION_FACTORY)

    val element = SAXBuilder().build(
      StringReader(
        "<" + ELEMENT_NAME + ">" +
        "<option name=\"" + SCRIPT_PATH_OPTION + "\" value=\"" + SCRIPT_PATH + "\" />" +
        "<option name=\"" + WORKING_DIRECTORY_PATH_OPTION + "\" value=\"" + WORKING_DIRECTORY_PATH + "\" />" +
        "</" + ELEMENT_NAME + ">"
      )
    ).rootElement

    runConfiguration.readExternal(element)

    TestCase.assertEquals(SCRIPT_PATH, runConfiguration.scriptPath)
    TestCase.assertEquals(WORKING_DIRECTORY_PATH, runConfiguration.workingDirectoryPath)
  }

  fun testWriteExternal() {
    val runConfiguration = RRunConfiguration(project, CONFIGURATION_FACTORY)
    runConfiguration.scriptPath = SCRIPT_PATH
    runConfiguration.workingDirectoryPath = WORKING_DIRECTORY_PATH

    val element = Element(ELEMENT_NAME)

    runConfiguration.writeExternal(element)

    TestCase.assertEquals(
      "<" + ELEMENT_NAME + ">" +
      "<option name=\"" + SCRIPT_PATH_OPTION + "\" value=\"" + SCRIPT_PATH + "\" />" +
      "<option name=\"" + WORKING_DIRECTORY_PATH_OPTION + "\" value=\"" + WORKING_DIRECTORY_PATH + "\" />" +
      "</" + ELEMENT_NAME + ">",
      XMLOutputter().outputString(element)
    )
  }

  fun testCopyParams() {
    val runConfiguration1 = RRunConfiguration(project, CONFIGURATION_FACTORY)
    runConfiguration1.scriptPath = SCRIPT_PATH
    runConfiguration1.workingDirectoryPath = WORKING_DIRECTORY_PATH

    val runConfiguration2 = RRunConfiguration(project, CONFIGURATION_FACTORY)
    RRunConfiguration.copyParams(runConfiguration1, runConfiguration2)
    TestCase.assertEquals(SCRIPT_PATH, runConfiguration2.scriptPath)
    TestCase.assertEquals(WORKING_DIRECTORY_PATH, runConfiguration2.workingDirectoryPath)
  }

  companion object {
    private const val SCRIPT_PATH = "s_path"
    private const val WORKING_DIRECTORY_PATH = "w_path"
    private const val ELEMENT_NAME = "CONFIGURATION"
    private const val SCRIPT_PATH_OPTION = "SCRIPT_PATH"
    private const val WORKING_DIRECTORY_PATH_OPTION = "WORKING_DIRECTORY_PATH"
    private val CONFIGURATION_FACTORY = mock(ConfigurationFactory::class.java)
  }
}