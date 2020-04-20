/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement

class RoxygenAnnotator : Annotator {
  override fun annotate(psiElement: PsiElement, holder: AnnotationHolder) {
    val visitor = RoxygenAnnotatorVisitor(holder)
    psiElement.accept(visitor)
  }
}
