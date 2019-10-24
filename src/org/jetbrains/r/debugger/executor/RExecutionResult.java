// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.debugger.executor;

import org.jetbrains.annotations.NotNull;

public class RExecutionResult {
    @NotNull
    private final String myOutput;
    @NotNull
    private final String myError;
    @NotNull
    private final RExecutionResultType myType;

    public RExecutionResult(@NotNull final String output,
                            @NotNull final String error,
                            @NotNull final RExecutionResultType type) {
        myOutput = output;
        myError = error;
        myType = type;
    }

    @NotNull
    public String getOutput() {
        return myOutput;
    }

    @NotNull
    public RExecutionResultType getType() {
        return myType;
    }

    @NotNull
    public String getError() {
        return myError;
    }
}
