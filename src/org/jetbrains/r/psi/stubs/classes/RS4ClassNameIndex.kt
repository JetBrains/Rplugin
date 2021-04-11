/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.stubs.classes

import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processor
import org.jetbrains.r.classes.s4.RS4ClassInfo
import org.jetbrains.r.psi.api.RCallExpression

class RS4ClassNameIndex : LibraryClassNameIndexBase<RS4ClassInfo>() {
  override val classKey = StubIndexKey.createIndexKey<String, RCallExpression>("R.s4class.shortName")

  override fun getKey(): StubIndexKey<String, RCallExpression> {
    return classKey
  }

  override fun callProcessingForDeclaration(rCallExpression: RCallExpression,
                                            processor: Processor<Pair<RCallExpression, RS4ClassInfo>>): Boolean {
    return rCallExpression.associatedS4ClassInfo?.let { processor.process(rCallExpression to it) } ?: true
  }

  override fun getClassInfoFromRCallExpression(rCallExpression: RCallExpression): RS4ClassInfo? {
    return rCallExpression.associatedS4ClassInfo
  }
}