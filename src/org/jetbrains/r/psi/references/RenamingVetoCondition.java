// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.references;

import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import org.jetbrains.r.psi.RPsiUtil;
import org.jetbrains.r.psi.api.RPsiElement;

/**
 * Prevent renaming of library methods
 * <p>
 * https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000062144-How-to-prevent-renaming-of-library-functions-
 *
 * @author Holger Brandl
 */
public class RenamingVetoCondition implements Condition<PsiElement> {

    @Override
    public boolean value(PsiElement psiElement) {
        return psiElement instanceof RPsiElement && RPsiUtil.INSTANCE.isLibraryElement(psiElement);
    }
}
