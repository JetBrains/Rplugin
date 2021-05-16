/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.s4

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import org.jetbrains.r.RLanguage
import org.jetbrains.r.classes.s4.classInfo.RS4ClassReferenceProvider
import org.jetbrains.r.psi.RElementFilters

class RS4ReferenceContributor : PsiReferenceContributor() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerReferenceProvider(
      PlatformPatterns.psiElement().withLanguage(RLanguage.INSTANCE).and(RElementFilters.STRING_FILTER),
      RS4ClassReferenceProvider, PsiReferenceRegistrar.HIGHER_PRIORITY
    )
  }
}