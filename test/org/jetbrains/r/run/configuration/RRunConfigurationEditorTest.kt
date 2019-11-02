// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class RRunConfigurationEditorTest {
  @Test
  fun resetApply() {
    val runConfiguration1 = createConfiguration("s_p_1")
    val runConfiguration2 = createConfiguration("s_p_2")
    val editor = RRunConfigurationEditor()
    editor.resetEditorFrom(runConfiguration1)
    editor.applyEditorTo(runConfiguration2)
    assertParamsEquals(runConfiguration1, runConfiguration2)
  }


  private fun createConfiguration(scriptPath: String): RRunConfiguration {
    val result = RRunConfiguration(PROJECT, CONFIGURATION_FACTORY)
    result.scriptPath = scriptPath
    return result
  }

  private fun assertParamsEquals(params1: RRunConfiguration, params2: RRunConfiguration) {
    assertEquals(params1.scriptPath, params2.scriptPath)
  }

  companion object {
    private val PROJECT = mock(Project::class.java)
    private val CONFIGURATION_FACTORY = mock(ConfigurationFactory::class.java)
  }
}