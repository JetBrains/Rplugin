// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r;

import com.intellij.codeInsight.generation.IndentedCommenter;
import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.psi.PsiComment;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.r.parsing.RParserDefinition;

public class RCommenter implements CodeDocumentationAwareCommenter, IndentedCommenter {
    @Override
    public String getLineCommentPrefix() {
        return "# ";
    }


    @Override
    public String getBlockCommentPrefix() {
        return null;
    }


    @Override
    public String getBlockCommentSuffix() {
        return null;
    }


    @Override
    public String getCommentedBlockCommentPrefix() {
        return null;
    }


    @Override
    public String getCommentedBlockCommentSuffix() {
        return null;
    }


    @Override
    public IElementType getLineCommentTokenType() {
        return RParserDefinition.END_OF_LINE_COMMENT;
    }


    @Override
    public IElementType getBlockCommentTokenType() {
        return null;
    }


    @Override
    public IElementType getDocumentationCommentTokenType() {
        return null;
    }


    @Override
    public String getDocumentationCommentPrefix() {
        return null;
    }


    @Override
    public String getDocumentationCommentLinePrefix() {
        return null;
    }


    @Override
    public String getDocumentationCommentSuffix() {
        return null;
    }


    @Override
    public boolean isDocumentationComment(PsiComment element) {
        return false;
    }


    @Nullable
    @Override
    public Boolean forceIndentedLineComment() {
        return true;
    }
}
