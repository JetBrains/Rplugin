// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r;

import com.intellij.lang.Language;

public class RLanguage extends Language {
    public static final RLanguage INSTANCE = new RLanguage();

  @Override
    public boolean isCaseSensitive() {
        return true; // http://jetbrains-feed.appspot.com/message/372001
    }


    protected RLanguage() {
        super("R");
    }
}
