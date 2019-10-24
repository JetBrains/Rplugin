// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.highlighting;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.r.parsing.RElementTypes;

public class RBraceMatcher implements PairedBraceMatcher {
    private final BracePair[] PAIRS = new BracePair[]{
            new BracePair(RElementTypes.R_LPAR, RElementTypes.R_RPAR, false),
            new BracePair(RElementTypes.R_LBRACKET, RElementTypes.R_RBRACKET, false),
            new BracePair(RElementTypes.R_LDBRACKET, RElementTypes.R_RDBRACKET, false),
            new BracePair(RElementTypes.R_LBRACE, RElementTypes.R_RBRACE, false)};


    @Override
    public BracePair[] getPairs() {
        return PAIRS;
    }


    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
        return true;
    }


    @Override
    public int getCodeConstructStart(final PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }
}
