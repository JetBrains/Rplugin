// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public final class RElementTypeFactory {
    private RElementTypeFactory() {
    }


    public static IElementType getElementTypeByName(@NotNull String name) {
        if (name.equals("R_ASSIGNMENT_STATEMENT")) {
            return new RAssignmentElementType(name);
        }
        if (name.equals("R_PARAMETER")) {
            return new RParameterElementType(name);
        }
        if (name.equals("R_CALL_EXPRESSION")) {
            return new RCallExpressionElementType(name);
        }
        throw new IllegalArgumentException("Unknown element type: " + name);
    }
}
