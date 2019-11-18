/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.references;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.interpreter.RInterpreter;
import org.jetbrains.r.interpreter.RInterpreterManager;
import org.jetbrains.r.rmarkdown.RMarkdownFileType;

final public class RSearchScopeUtil {
  private RSearchScopeUtil() {}

  public static GlobalSearchScope getScope(PsiElement element) {

    return new DelegatingGlobalSearchScope(GlobalSearchScope.allScope(element.getProject())) {
      @Override
      public boolean contains(@NotNull VirtualFile file) {
        if (file.equals(PsiUtilBase.getVirtualFile(element))) {
          return true;
        }
        if (file.getFileType() == RMarkdownFileType.INSTANCE) {
          return false;
        }
        if (myBaseScope.contains(file)) {
          return true;
        }
        RInterpreter interpreter = RInterpreterManager.Companion.getInterpreter(element.getProject());
        if (interpreter == null) {
          return false;
        }
        return interpreter.getSkeletonRoots().contains(file.getParent());
      }
    };
  }
}
