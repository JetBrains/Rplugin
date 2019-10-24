// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.configuration;

import com.intellij.execution.configurations.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.RIconsKt;

public class RRunConfigurationType extends ConfigurationTypeBase {

    public RRunConfigurationType() {
        super(
                "RRunConfigurationType",
                "R",
                "R run configuration",
                RIconsKt.R_LOGO_16
        );

        addFactory(new RConfigurationFactory(this));
    }


    @NotNull
    public static RRunConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(RRunConfigurationType.class);
    }


    @NotNull
    public ConfigurationFactory getMainFactory() {
        return getConfigurationFactories()[0];
    }


    private static class RConfigurationFactory extends ConfigurationFactory {

        public RConfigurationFactory(@NotNull final ConfigurationType configurationType) {
            super(configurationType);
        }


        @NotNull
        @Override
        public RunConfiguration createTemplateConfiguration(@NotNull final Project project) {
            return new RRunConfiguration(project, this);
        }
    }
}
