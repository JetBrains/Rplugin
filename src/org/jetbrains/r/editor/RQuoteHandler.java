// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor;

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import org.jetbrains.r.parsing.RElementTypes;

public class RQuoteHandler extends SimpleTokenSetQuoteHandler {
    public RQuoteHandler() {
        super(RElementTypes.R_STRING);
    }


    @Override
    public boolean hasNonClosedLiteral(Editor editor, HighlighterIterator iterator, int offset) {
        final Document document = editor.getDocument();
        int lineEndOffset = document.getLineEndOffset(document.getLineNumber(offset));
        if (offset < lineEndOffset) {
            final CharSequence charSequence = document.getCharsSequence();
            final char openQuote = charSequence.charAt(offset);
            final int nextCharOffset = offset + 1;
            if (nextCharOffset < lineEndOffset && charSequence.charAt(nextCharOffset) == openQuote) {
                return true;
            }
            for (int i = nextCharOffset + 1; i < lineEndOffset; i++) {
                if (charSequence.charAt(i) == openQuote) {
                    return false;
                }
            }
        }
        return true;
    }
}
