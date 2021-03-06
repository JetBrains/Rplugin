// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.stubs;

import com.intellij.psi.stubs.NamedStub;
import org.jetbrains.r.psi.api.RAssignmentStatement;

public interface RAssignmentStub extends NamedStub<RAssignmentStatement> {
    boolean isFunctionDeclaration();

    boolean isPrimitiveFunctionDeclaration();

    boolean isRight();

    boolean isTopLevelAssignment();
}
