// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.psi.tree.TokenSet;

public class RLexer extends MergingLexerAdapter {
    private static final TokenSet TOKENS_TO_MERGE = TokenSet.WHITE_SPACE;

    public RLexer() {
        super(new FlexAdapter(new _RLexer(null)), TOKENS_TO_MERGE);
    }
}
