/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r

import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.psi.references.RReferenceBase

abstract class RLightCodeInsightFixtureTestCase: RUsefulTestCase() {
  protected fun resolve(): Array<ResolveResult> {
    val reference = myFixture.file.findReferenceAt(myFixture.caretOffset)
    val rReferenceBase = reference as RReferenceBase<*>
    return rReferenceBase.multiResolve(false)
  }

  protected fun <T : PsiElement> findElementAtCaret(aClass: Class<T>): T? {
     return PsiTreeUtil.getParentOfType(myFixture.file.findElementAt(myFixture.caretOffset), aClass, false)
  }
}