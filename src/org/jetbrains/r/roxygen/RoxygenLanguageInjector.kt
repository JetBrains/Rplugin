/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen

import com.intellij.lang.LanguageParserDefinitions
import com.intellij.openapi.util.TextRange
import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.LanguageInjector
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.tree.injected.InjectionBackgroundSuppressor

class RoxygenLanguageInjector : LanguageInjector {
  override fun getLanguagesToInject(host: PsiLanguageInjectionHost,
                                    injectionPlacesRegistrar: InjectedLanguagePlaces) {
    if (host is RoxygenCommentPlaceholder) {
      LanguageParserDefinitions.INSTANCE.forLanguage(RoxygenLanguage.INSTANCE)?.let {
        injectionPlacesRegistrar.addPlace(RoxygenLanguage.INSTANCE, TextRange(0, host.getTextLength()), null, null)
      }
    }
  }
}

interface RoxygenCommentPlaceholder : PsiDocCommentBase, InjectionBackgroundSuppressor