// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.configuration

import com.intellij.openapi.options.ConfigurationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.*

class RRunConfigurationUtilsTest {
  @Test(expected = ConfigurationException::class)
  @Throws(ConfigurationException::class)
  fun checkConfigurationWithoutScriptPath() {
    val runConfiguration = mock(RRunConfiguration::class.java)
    `when`(runConfiguration.scriptPath).thenReturn("")
    RRunConfigurationUtils.checkConfiguration(runConfiguration)
  }

  @Test
  @Throws(ConfigurationException::class)
  fun checkConfiguration() {
    val runConfiguration = mock(RRunConfiguration::class.java)
    `when`(runConfiguration.scriptPath).thenReturn("s_p")
    RRunConfigurationUtils.checkConfiguration(runConfiguration)
    verify(runConfiguration, times(1)).scriptPath
    verifyNoMoreInteractions(runConfiguration)
  }

  @Test
  fun suggestedNameForUnknownScriptPath() {
    val runConfiguration = mock(RRunConfiguration::class.java)
    `when`(runConfiguration.scriptPath).thenReturn("")
    assertNull(RRunConfigurationUtils.suggestedName(runConfiguration))
    verify(runConfiguration, times(1)).scriptPath
    verifyNoMoreInteractions(runConfiguration)
  }

  @Test
  fun suggestedNameForNotRScript() {
    val runConfiguration = mock(RRunConfiguration::class.java)
    `when`(runConfiguration.scriptPath).thenReturn("script.s")
    assertEquals("script.s", RRunConfigurationUtils.suggestedName(runConfiguration))
    verify(runConfiguration, times(1)).scriptPath
    verifyNoMoreInteractions(runConfiguration)
  }

  @Test
  fun suggestedNameForRScript() {
    val runConfiguration = mock(RRunConfiguration::class.java)
    `when`(runConfiguration.scriptPath).thenReturn("script.r")
    assertEquals("script", RRunConfigurationUtils.suggestedName(runConfiguration))
    verify(runConfiguration, times(1)).scriptPath
    verifyNoMoreInteractions(runConfiguration)
  }
}