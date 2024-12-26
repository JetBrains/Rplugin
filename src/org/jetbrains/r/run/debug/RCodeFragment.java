// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.psi.RFileImpl;

class RCodeFragment extends RFileImpl {

    public RCodeFragment(final @NotNull Project project, final @NotNull String name, final @NotNull String text) {
        super(createLightVirtualFileViewProvider(project, name, text));

        ((SingleRootFileViewProvider) getViewProvider()).forceCachedPsi(this);
    }


    private static @NotNull FileViewProvider createLightVirtualFileViewProvider(final @NotNull Project project,
                                                                                final @NotNull String name,
                                                                                final @NotNull String text) {
        return getFileManager(project).createFileViewProvider(
                createLightVirtualFile(name, text), true
        );
    }


    private static @NotNull FileManager getFileManager(final @NotNull Project project) {
        return PsiManagerEx.getInstanceEx(project).getFileManager();
    }


    private static @NotNull LightVirtualFile createLightVirtualFile(final @NotNull String name, final @NotNull String text) {
        return new LightVirtualFile(name, getFileType(name), text);
    }


    private static @NotNull FileType getFileType(final @NotNull String name) {
        return FileTypeManager.getInstance().getFileTypeByFileName(name);
    }
}
