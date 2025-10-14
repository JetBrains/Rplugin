/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.classes.s4.context.setClass

import com.intellij.r.psi.classes.s4.context.RS4Context

abstract class RS4SetClassContext : RS4Context {
  override val functionName = "setClass"
}