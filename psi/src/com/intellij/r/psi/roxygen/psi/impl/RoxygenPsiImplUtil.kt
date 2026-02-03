/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.r.psi.roxygen.psi.impl

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.tree.LeafElement
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.ROXYGEN_IDENTIFIER
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.ROXYGEN_TAG_NAME
import com.intellij.r.psi.roxygen.psi.api.RoxygenExpression
import com.intellij.r.psi.roxygen.psi.api.RoxygenIdentifierExpression
import com.intellij.r.psi.roxygen.psi.api.RoxygenNamespaceAccessExpression
import com.intellij.r.psi.roxygen.psi.api.RoxygenParameter
import com.intellij.r.psi.roxygen.psi.api.RoxygenPsiElement
import com.intellij.r.psi.roxygen.psi.api.RoxygenTag
import com.intellij.r.psi.roxygen.reference.RoxygenHelpPageReference
import com.intellij.r.psi.roxygen.reference.RoxygenParameterReference

object RoxygenPsiImplUtil {
  const val UNKNOWN = "<unknown>"

  @JvmStatic
  fun getName(tag: RoxygenTag): String {
    return StringUtil.trimStart(getOriginalName(tag), "@")
  }

  @JvmStatic
  fun getOriginalName(tag: RoxygenTag): String {
    val tagName = tag.node.findChildByType(ROXYGEN_TAG_NAME)
    return tagName?.text ?: UNKNOWN
  }

  @JvmStatic
  fun getName(element: RoxygenIdentifierExpression): String = element.text

  @JvmStatic
  fun setName(element: RoxygenIdentifierExpression, newName: String): PsiElement = setNameToIdentifier(element, newName)

  @JvmStatic
  fun getName(element: RoxygenParameter): String = element.text

  @JvmStatic
  fun setName(element: RoxygenParameter, newName: String): PsiElement = setNameToIdentifier(element, newName)

  @JvmStatic
  fun getReference(psiElement: RoxygenExpression): PsiReference? = null

  @JvmStatic
  fun getReference(psiElement: RoxygenParameter): PsiReference {
    val range = psiElement.node.textRange.shiftLeft(psiElement.node.startOffset)
    return RoxygenParameterReference(psiElement, range)
  }

  @JvmStatic
  fun getReference(psiElement: RoxygenIdentifierExpression): PsiReference {
    val range = psiElement.node.textRange.shiftLeft(psiElement.node.startOffset)
    return RoxygenHelpPageReference(psiElement, range)
  }

  @JvmStatic
  fun getNamespaceName(referenceExpression: RoxygenNamespaceAccessExpression): String {
    return referenceExpression.namespace.text
  }

  private fun setNameToIdentifier(element: RoxygenPsiElement, newName: String): PsiElement {
    val node = element.node.findChildByType(ROXYGEN_IDENTIFIER)
    (node as? LeafElement)?.replaceWithText(newName)
    return element
  }
}