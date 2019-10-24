// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;

/**
 * @author holgerbrandl
 */
public class RParserUtil extends GeneratedParserUtilBase {
    public static boolean parseEmptyExpression(PsiBuilder builder, int level) {
        PsiBuilder.Marker emptyMarker = builder.mark();
        emptyMarker.done(RElementTypes.R_EMPTY_EXPRESSION);
        return true;
    }
}
