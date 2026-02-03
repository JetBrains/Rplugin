/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen.annotator

import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.r.psi.roxygen.AUTOLINK
import com.intellij.r.psi.roxygen.HELP_PAGE_LINK
import com.intellij.r.psi.roxygen.LINK_DESTINATION
import com.intellij.r.psi.roxygen.PARAMETER
import com.intellij.r.psi.roxygen.psi.api.RoxygenAutolink
import com.intellij.r.psi.roxygen.psi.api.RoxygenHelpPageLink
import com.intellij.r.psi.roxygen.psi.api.RoxygenLinkDestination
import com.intellij.r.psi.roxygen.psi.api.RoxygenParamTag
import com.intellij.r.psi.roxygen.psi.api.RoxygenVisitor


class RoxygenAnnotatorVisitor(private val holder: AnnotationHolder) : RoxygenVisitor() {

  override fun visitHelpPageLink(o: RoxygenHelpPageLink) {
    highlight(o, HELP_PAGE_LINK)
  }

  override fun visitParamTag(o: RoxygenParamTag) {
    o.parameters.forEach { highlight(it, PARAMETER) }
  }

  override fun visitLinkDestination(o: RoxygenLinkDestination) {
    highlight(o, LINK_DESTINATION)
  }

  override fun visitAutolink(o: RoxygenAutolink) {
    highlight(o, AUTOLINK)
  }

  private fun highlight(element: PsiElement, colorKey: TextAttributesKey) {
    val annotationText = if (ApplicationManager.getApplication().isUnitTestMode) colorKey.externalName else null
    val annotationBuilder: AnnotationBuilder
    if (annotationText == null) {
      annotationBuilder = holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
    }
    else {
      annotationBuilder = holder.newAnnotation(HighlightSeverity.INFORMATION, annotationText)
    }
    annotationBuilder.range(element.textRange).create()
  }
}