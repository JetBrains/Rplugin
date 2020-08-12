/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.util.IncorrectOperationException

fun Disposable.tryRegisterDisposable(child: Disposable) {
  if (!Disposer.tryRegister(this, child)) {
    Disposer.dispose(child)
  }
}
