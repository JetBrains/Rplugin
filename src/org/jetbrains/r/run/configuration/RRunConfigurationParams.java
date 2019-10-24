// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.configuration;

import org.jetbrains.annotations.NotNull;

interface RRunConfigurationParams {

    @NotNull
    String getScriptPath();


    void setScriptPath(@NotNull final String scriptPath);


    @NotNull
    String getWorkingDirectoryPath();


    void setWorkingDirectoryPath(@NotNull final String workingDirectoryPath);
}

