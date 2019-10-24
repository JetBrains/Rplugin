/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.references

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.ResolveResult
import com.intellij.util.IncorrectOperationException
import org.jetbrains.r.console.RConsoleRuntimeInfo
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.packages.RPackage
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.RPsiElement

abstract class RReferenceBase<T : RPsiElement>(protected val psiElement: T) : PsiPolyVariantReference {

  @Throws(IncorrectOperationException::class)
  override fun bindToElement(element: PsiElement) = null

  override fun isReferenceTo(element: PsiElement): Boolean {
    val resolve = resolve()
    return if (resolve is PsiNameIdentifierOwner) {
      resolve === element || resolve.identifyingElement === element
    } else {
      resolve === element
    }
  }

  override fun isSoft() = false

  override fun getCanonicalText() = element.text!!

  override fun getElement() = psiElement

  override fun getRangeInElement() = psiElement.node.textRange.shiftRight(-psiElement.node.startOffset)

  override fun handleElementRename(newElementName: String): PsiElement? = null

  override fun resolve(): PsiElement? {
    val results = multiResolve(false)
    return if (results.size == 1) results.first().element else null
  }

  override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
    val resolveResults = multiResolveInner(incompleteCode)
    return psiElement.containingFile.runtimeInfo?.let { info -> resolveUsingRuntimeInfo(info, resolveResults) } ?: resolveResults
  }

  private fun resolveUsingRuntimeInfo(runtimeInfo: RConsoleRuntimeInfo,
                                      resolveResults: Array<ResolveResult>): Array<ResolveResult> {
    val loadedNamespaces = runtimeInfo.loadedPackages.mapIndexed { index, s -> s to index }.toMap()
    val topResolveResult = resolveResults
                             .filter { it.element is RPsiElement && RPsiUtil.isLibraryElement(it.element as RPsiElement) }
                             .minBy { getLoadingNumber(loadedNamespaces, it) } ?: return resolveResults
    if (getLoadingNumber(loadedNamespaces, topResolveResult) != Int.MAX_VALUE) {
      return arrayOf(topResolveResult)
    }
    return resolveResults
  }

  private fun getLoadingNumber(loadedNamespaces: Map<String, Int>, result: ResolveResult) =
    loadedNamespaces[RPackage.getOrCreate(result.element!!.containingFile)?.packageName] ?: Int.MAX_VALUE

  protected abstract fun multiResolveInner(incompleteCode: Boolean): Array<ResolveResult>
}