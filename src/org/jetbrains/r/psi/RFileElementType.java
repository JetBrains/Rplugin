// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi;

import com.intellij.psi.tree.IStubFileElementType;
import org.jetbrains.r.RLanguage;

public class RFileElementType extends IStubFileElementType {
    public RFileElementType() {
      super("R", RLanguage.INSTANCE);
    }

    @Override
    public int getStubVersion() {
        return 13;
    }
}
