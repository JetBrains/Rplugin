/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi.stubs.classes

import com.intellij.psi.stubs.StubIndexKey
import com.intellij.r.psi.classes.s4.classInfo.RS4ClassInfo
import com.intellij.r.psi.classes.s4.classInfo.associatedS4ClassInfo
import com.intellij.r.psi.psi.api.RCallExpression

open class RS4ClassNameIndex : LibraryClassNameIndex<RS4ClassInfo>() {
  override fun getKey(): StubIndexKey<String, RCallExpression> = KEY
  override val mapClassInfoFunction = RCallExpression::associatedS4ClassInfo

  companion object : RS4ClassNameIndex() {
    private val KEY = StubIndexKey.createIndexKey<String, RCallExpression>("R.s4class.shortName")
  }
}