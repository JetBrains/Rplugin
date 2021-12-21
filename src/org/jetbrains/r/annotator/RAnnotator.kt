/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.annotator

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.injected.changesHandler.range

class RAnnotator : Annotator {
  override fun annotate(psiElement: PsiElement, holder: AnnotationHolder) {
    val infos = ArrayList<HighlightInfo>()
    val visitor = RAnnotatorVisitor(infos, holder.currentAnnotationSession)
    psiElement.accept(visitor)
    for (info: HighlightInfo in infos) {
      val builder: AnnotationBuilder
      if (info.description == null) {
        builder = holder.newSilentAnnotation(info.severity)
      }
      else {
        builder = holder.newAnnotation(info.severity, info.description)
      }
      builder.range(info.range)
      if (info.forcedTextAttributesKey != null) {
        builder.textAttributes(info.forcedTextAttributesKey)
      }
      if (info.forcedTextAttributes != null) {
        builder.enforcedTextAttributes(info.forcedTextAttributes)
      }
      builder.create()
    }
  }
}
