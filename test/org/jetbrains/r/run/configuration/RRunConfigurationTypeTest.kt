// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.configuration

import com.intellij.openapi.project.Project
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class RRunConfigurationTypeTest {

  @Test
  fun idNameDescription() {
    val configurationType = RRunConfigurationType()
    assertEquals("RRunConfigurationType", configurationType.id)
    assertEquals("R", configurationType.displayName)
    assertEquals("R run configuration", configurationType.configurationTypeDescription)
  }

  @Test
  fun template() {
    val project = mock(Project::class.java)
    val configurationFactory = RRunConfigurationType().mainFactory
    val templateConfiguration = configurationFactory.createTemplateConfiguration(project) as RRunConfiguration
    assertEquals("", templateConfiguration.scriptPath)
  }
}