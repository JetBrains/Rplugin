/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.r.psi.roxygen

import com.intellij.lang.Language

class RoxygenLanguage private constructor() : Language("Roxygen") {

  override fun isCaseSensitive(): Boolean = true

  companion object {
    val INSTANCE = RoxygenLanguage()
  }
}