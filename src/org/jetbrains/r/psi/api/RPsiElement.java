// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.api;

import com.intellij.psi.NavigatablePsiElement;

public interface RPsiElement extends NavigatablePsiElement {
    /**
     * An empty array to return cheaply without allocating it anew.
     */
    RPsiElement[] EMPTY_ARRAY = new RPsiElement[0];
}
