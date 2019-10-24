// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.configuration;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.project.Project;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class RRunConfigurationTypeTest {

    @Test
    public void idNameDescription() {
        final RRunConfigurationType configurationType = new RRunConfigurationType();

        assertEquals("RRunConfigurationType", configurationType.getId());
        assertEquals("R", configurationType.getDisplayName());
        assertEquals("R run configuration", configurationType.getConfigurationTypeDescription());
    }


    @Test
    public void template() {
        final Project project = mock(Project.class);

        final ConfigurationFactory configurationFactory = new RRunConfigurationType().getMainFactory();
        final RRunConfiguration templateConfiguration = (RRunConfiguration) configurationFactory.createTemplateConfiguration(project);

        assertEquals("", templateConfiguration.getScriptPath());
        assertEquals("", templateConfiguration.getWorkingDirectoryPath());
    }
}