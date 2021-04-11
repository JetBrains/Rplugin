/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.stubs.classes

import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processor
import org.jetbrains.r.classes.r6.R6ClassInfo
import org.jetbrains.r.psi.api.RCallExpression

class R6ClassNameIndex : LibraryClassNameIndexBase<R6ClassInfo>() {
  override val classKey = StubIndexKey.createIndexKey<String, RCallExpression>("R.r6class.shortName")

  override fun getKey(): StubIndexKey<String, RCallExpression> {
    return classKey
  }

  override fun callProcessingForDeclaration(rCallExpression: RCallExpression,
                                            processor: Processor<Pair<RCallExpression, R6ClassInfo>>): Boolean {
    return rCallExpression.associatedR6ClassInfo?.let { processor.process(rCallExpression to it) } ?: true
  }

  override fun getClassInfoFromRCallExpression(rCallExpression: RCallExpression): R6ClassInfo? {
    return rCallExpression.associatedR6ClassInfo
  }
}