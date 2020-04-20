/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen.psi.impl

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.r.roxygen.parsing.RoxygenElementTypes.*
import org.jetbrains.r.roxygen.psi.api.*

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
  fun setName(element: RoxygenIdentifierExpression, newName: String): PsiElement {
    val node = element.node.findChildByType(ROXYGEN_IDENTIFIER)
    (node as? LeafElement)?.replaceWithText(newName)
    return element
  }

  @JvmStatic
  fun getName(element: RoxygenParameter): String = element.text

  @JvmStatic
  fun setName(element: RoxygenParameter, newName: String): PsiElement {
    val node = element.node.findChildByType(ROXYGEN_PARAM_TAG)
    (node as? LeafElement)?.replaceWithText(newName)
    return element
  }

  @JvmStatic
  fun getReference(psi: RoxygenExpression): PsiReference? = null

  @JvmStatic
  fun getNamespaceName(referenceExpression: RoxygenNamespaceAccessExpression): String {
    return referenceExpression.namespace.text
  }
}