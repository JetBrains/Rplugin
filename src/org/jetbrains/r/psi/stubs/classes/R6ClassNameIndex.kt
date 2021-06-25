/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.stubs.classes

import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.r.classes.r6.R6ClassInfo
import org.jetbrains.r.classes.r6.associatedR6ClassInfo
import org.jetbrains.r.psi.api.RCallExpression

open class R6ClassNameIndex : LibraryClassNameIndex<R6ClassInfo>() {
  override fun getKey(): StubIndexKey<String, RCallExpression> = KEY
  override val mapClassInfoFunction = RCallExpression::associatedR6ClassInfo

  companion object : R6ClassNameIndex() {
    private val KEY = StubIndexKey.createIndexKey<String, RCallExpression>("R.r6class.shortName")
  }
}