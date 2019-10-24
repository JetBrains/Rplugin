// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;

public class RElementGenerator {

    public static PsiFile createDummyFile(String contents, boolean physical, Project project) {
        final PsiFileFactory factory = PsiFileFactory.getInstance(project);
        final String name = "dummy." + RFileType.INSTANCE.getDefaultExtension();
        final LightVirtualFile virtualFile = new LightVirtualFile(name, RFileType.INSTANCE, contents);
        final PsiFile psiFile = ((PsiFileFactoryImpl) factory).trySetupPsiForFile(virtualFile, RLanguage.INSTANCE, physical, true);
        assert psiFile != null;
        return psiFile;
    }
}
