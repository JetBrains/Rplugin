package org.jetbrains.r.injections

import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.r.psi.RLanguage
import org.intellij.plugins.intelliLang.inject.AbstractLanguageInjectionSupport

class RInjectionSupport : AbstractLanguageInjectionSupport() {
  override fun getId(): String {
    return "R"
  }

  override fun getPatternClasses(): Array<Class<*>> {
    return arrayOf(RPatterns::class.java)
  }

  override fun isApplicableTo(host: PsiLanguageInjectionHost?): Boolean {
    return host != null && host.language == RLanguage.INSTANCE
  }

  override fun useDefaultInjector(host: PsiLanguageInjectionHost?): Boolean {
    return true
  }
}