// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.r.psi.RLanguage;
import com.intellij.r.psi.psi.api.RCallExpression;
import com.intellij.r.psi.psi.api.RIdentifierExpression;
import com.intellij.r.psi.psi.api.RPsiElement;
import org.jetbrains.annotations.Nullable;


/**
 * @author brandl
 */
public final class RElementFactory {
  private static Logger ourLogger = Logger.getInstance(RElementFactory.class);

  private RElementFactory() {
  }

  public static PsiElement createLeafFromText(Project project, String text) {
    PsiFile fileFromText = buildRFileFromText(project, text);
    return PsiTreeUtil.getDeepestFirst(fileFromText);
  }

  public static RPsiElement createRPsiElementFromText(Project project, String text) {
    PsiFile fromText = buildRFileFromText(project, text);
    return (RPsiElement)fromText.getFirstChild();
  }

  public static @Nullable RPsiElement createRPsiElementFromTextOrNull(Project project, String text) {
    PsiFile fromText = buildRFileFromText(project, text);
    if (fromText.getFirstChild() instanceof RPsiElement) {
      return (RPsiElement)fromText.getFirstChild();
    }
    else {
      ourLogger.error("Cannot build psi element: " + text);
      return null;
    }
  }


  public static RCallExpression createFuncallFromText(Project project, String text) {
    return (RCallExpression)createRPsiElementFromText(project, text);
  }

  public static RIdentifierExpression createRefExpression(Project project, String text) {
    return (RIdentifierExpression)createRPsiElementFromText(project, text);
  }

  public static PsiFile buildRFileFromText(Project project, String text) {
    return PsiFileFactory.getInstance(project).createFileFromText("a.R", RLanguage.INSTANCE, text);
  }
}
