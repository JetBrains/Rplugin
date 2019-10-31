/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.skeleton.psi

import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.PsiElementBase
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.util.IncorrectOperationException
import org.jetbrains.r.RLanguage

abstract class RSkeletonBase : PsiElementBase(), PsiCompiledElement {

  override fun getLanguage(): Language = RLanguage.INSTANCE

  override fun getChildren(): Array<PsiElement> {
    return emptyArray()
  }

  override fun getManager(): PsiManager {
    return containingFile?.manager ?: throw PsiInvalidElementAccessException(this)
  }

  override fun isPhysical(): Boolean = false

  override fun getNode(): ASTNode {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun copy(): PsiElement {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun getPrevSibling(): PsiElement {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun processDeclarations(processor: PsiScopeProcessor, state: ResolveState, lastParent: PsiElement?, place: PsiElement): Boolean {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }


  override fun getTextRange(): TextRange {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun getStartOffsetInParent(): Int {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun getTextLength(): Int {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun findElementAt(offset: Int): PsiElement? {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun getTextOffset(): Int {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun textToCharArray(): CharArray {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }
}
