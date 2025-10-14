/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.classes.s4

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.r.psi.RLanguage
import com.intellij.r.psi.classes.s4.classInfo.RS4ClassReferenceProvider
import com.intellij.r.psi.psi.RElementFilters

class RS4ReferenceContributor : PsiReferenceContributor() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerReferenceProvider(
      PlatformPatterns.psiElement().withLanguage(RLanguage.INSTANCE).and(RElementFilters.STRING_FILTER),
      RS4ClassReferenceProvider, PsiReferenceRegistrar.HIGHER_PRIORITY
    )
  }
}