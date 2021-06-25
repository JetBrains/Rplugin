/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.stubs.classes

import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.r.classes.s4.classInfo.RS4ClassInfo
import org.jetbrains.r.classes.s4.classInfo.associatedS4ClassInfo
import org.jetbrains.r.psi.api.RCallExpression

open class RS4ClassNameIndex : LibraryClassNameIndex<RS4ClassInfo>() {
  override fun getKey(): StubIndexKey<String, RCallExpression> = KEY
  override val mapClassInfoFunction = RCallExpression::associatedS4ClassInfo

  companion object : RS4ClassNameIndex() {
    private val KEY = StubIndexKey.createIndexKey<String, RCallExpression>("R.s4class.shortName")
  }
}