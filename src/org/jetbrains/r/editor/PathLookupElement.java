// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class PathLookupElement extends LookupElement {

    private final File file;


    PathLookupElement(File file) {
        this.file = file;
    }


    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(file.getPath());
        presentation.setIcon(file.isDirectory() ? PlatformIcons.DIRECTORY_CLOSED_ICON : PlatformIcons.FILE_ICON);
    }

    @NotNull
    @Override
    public String getLookupString() {
        return file.getName();
    }

    @Override
    public boolean isCaseSensitive() {
        return true;
    }
}
