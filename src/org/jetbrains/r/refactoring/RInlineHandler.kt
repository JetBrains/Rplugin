// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring

import com.intellij.lang.refactoring.InlineHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap

/**
 * @author Holger Brandl
 */
class RInlineHandler : InlineHandler {

  override fun prepareInlineElement(psiElement: PsiElement, editor: Editor?, b: Boolean): InlineHandler.Settings? {
    return InlineHandler.Settings.CANNOT_INLINE_SETTINGS
  }

  override fun removeDefinition(psiElement: PsiElement, settings: InlineHandler.Settings) {
    psiElement.delete()
  }

  override fun createInliner(psiElement: PsiElement, settings: InlineHandler.Settings): InlineHandler.Inliner? {
    //        if (element instanceof GrVariable) {
    //            return new GrVariableInliner((GrVariable)element, settings);
    //        }
    //        if (element instanceof GrMethod) {
    //            return new GroovyMethodInliner((GrMethod)element);
    //        }
    return object : InlineHandler.Inliner {
      override fun getConflicts(psiReference: PsiReference, psiElement: PsiElement): MultiMap<PsiElement, String>? {
        return MultiMap.empty()
      }

      override fun inlineUsage(usageInfo: UsageInfo, psiElement: PsiElement) {
        println("sdf")
      }
    }
  }
}
