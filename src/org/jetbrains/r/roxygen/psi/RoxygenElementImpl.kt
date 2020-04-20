/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.r.roxygen.RoxygenLanguage
import org.jetbrains.r.roxygen.psi.api.RoxygenPsiElement

/**
 * Common base class for all Roxygen containers.
 */
open class RoxygenElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), RoxygenPsiElement {

  override fun getLanguage(): Language = RoxygenLanguage.INSTANCE

  override fun toString(): String {
    return node.elementType.toString()
  }

  override fun accept(visitor: PsiElementVisitor) {
    visitor.visitElement(this)
  }
}