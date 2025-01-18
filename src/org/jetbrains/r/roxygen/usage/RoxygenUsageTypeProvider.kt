/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen.usage

import com.intellij.psi.PsiElement
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProvider
import org.jetbrains.r.RBundle
import org.jetbrains.r.roxygen.RoxygenLanguage

private class RoxygenUsageTypeProvider : UsageTypeProvider {
  override fun getUsageType(element: PsiElement): UsageType? {
    return if (element.language === RoxygenLanguage.INSTANCE) ROXYGEN_DOCUMENTATION
    else null
  }

  companion object {
    val ROXYGEN_DOCUMENTATION by lazy { UsageType(RBundle.message("usage.in.roxygen")) }
  }
}