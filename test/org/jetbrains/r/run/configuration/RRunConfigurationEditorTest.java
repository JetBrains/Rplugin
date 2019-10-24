// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.configuration;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.HashMap;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class RRunConfigurationEditorTest {

    @NotNull
    private static final Project PROJECT = mock(Project.class);

    @NotNull
    private static final ConfigurationFactory CONFIGURATION_FACTORY = mock(ConfigurationFactory.class);


    @Test
    public void resetApply() {
        final RRunConfiguration runConfiguration1 = createConfiguration("s_p_1", "w_d_p_1");
        final RRunConfiguration runConfiguration2 = createConfiguration("s_p_2", "w_d_p_2");

        final RRunConfigurationEditor editor = new RRunConfigurationEditor(PROJECT);
        editor.resetEditorFrom(runConfiguration1);
        editor.applyEditorTo(runConfiguration2);

        assertParamsEquals(runConfiguration1, runConfiguration2);
    }


    @NotNull
    private RRunConfiguration createConfiguration(@NotNull final String scriptPath,
                                                  @NotNull final String workingDirectoryPath) {
        final RRunConfiguration result = new RRunConfiguration(PROJECT, CONFIGURATION_FACTORY);

        final Map<String, String> envs = new HashMap<String, String>();

        result.setScriptPath(scriptPath);
        result.setWorkingDirectoryPath(workingDirectoryPath);

        return result;
    }


    private void assertParamsEquals(@NotNull final RRunConfigurationParams params1, @NotNull final RRunConfigurationParams params2) {
        assertEquals(params1.getScriptPath(), params2.getScriptPath());
        assertEquals(params1.getWorkingDirectoryPath(), params2.getWorkingDirectoryPath());
    }
}