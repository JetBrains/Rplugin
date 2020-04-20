/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import org.jetbrains.r.roxygen.AUTOLINK
import org.jetbrains.r.roxygen.HELP_PAGE_LINK
import org.jetbrains.r.roxygen.LINK_DESTINATION
import org.jetbrains.r.roxygen.PARAMETER
import org.jetbrains.r.roxygen.psi.api.*


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
    holder.createInfoAnnotation(element, annotationText).textAttributes = colorKey
  }
}