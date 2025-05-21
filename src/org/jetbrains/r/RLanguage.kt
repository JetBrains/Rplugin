// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r

import com.intellij.lang.Language

class RLanguage private constructor() : Language("R") {
  override fun isCaseSensitive(): Boolean = true

  companion object {
    @JvmField
    val INSTANCE: RLanguage = RLanguage()
  }
}
