/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.references

import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import org.jetbrains.r.interpreter.RInterpreterStateManager
import org.jetbrains.r.psi.api.RPsiElement

class RImportReference(psiElement: RPsiElement): RReferenceBase<RPsiElement>(psiElement) {
  override fun multiResolveInner(incompleteCode: Boolean): Array<ResolveResult> {
    val state = RInterpreterStateManager.getCurrentStateOrNull(element.project) ?: return emptyArray()
    val packageName = psiElement.name ?: return emptyArray()
    return arrayOf(PsiElementResolveResult(state.getSkeletonFileByPackageName(packageName) ?: return emptyArray()))
  }
}