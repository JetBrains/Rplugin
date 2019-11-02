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
        "</" + ELEMENT_NAME + ">"
      )
    ).rootElement
    runConfiguration.readExternal(element)
    TestCase.assertEquals(SCRIPT_PATH, runConfiguration.scriptPath)
  }

  fun testWriteExternal() {
    val runConfiguration = RRunConfiguration(project, CONFIGURATION_FACTORY)
    runConfiguration.scriptPath = SCRIPT_PATH
    val element = Element(ELEMENT_NAME)
    runConfiguration.writeExternal(element)
    TestCase.assertEquals(
      "<" + ELEMENT_NAME + ">" +
      "<option name=\"" + SCRIPT_PATH_OPTION + "\" value=\"" + SCRIPT_PATH + "\" />" +
      "</" + ELEMENT_NAME + ">",
      XMLOutputter().outputString(element)
    )
  }

  companion object {
    private const val SCRIPT_PATH = "s_path"
    private const val ELEMENT_NAME = "CONFIGURATION"
    private const val SCRIPT_PATH_OPTION = "SCRIPT_PATH"
    private val CONFIGURATION_FACTORY = mock(ConfigurationFactory::class.java)
  }
}