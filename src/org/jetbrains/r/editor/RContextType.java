// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.editor;

import com.intellij.codeInsight.template.FileTypeBasedContextType;
import com.intellij.lang.Language;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.RFileType;
import org.jetbrains.r.RLanguage;


public class RContextType extends FileTypeBasedContextType {

    public RContextType() {
        super("R", "&R", RFileType.INSTANCE); //NON-NLS
    }


    @Override
    public boolean isInContext(@NotNull PsiFile file, int offset) {
        return isMyLanguage(file.getLanguage());
    }


    private static boolean isMyLanguage(Language language) {
      return language.isKindOf(RLanguage.INSTANCE);
    }
}