/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.s4.context.setClass

import org.jetbrains.r.classes.s4.context.RS4Context

abstract class RS4SetClassContext : RS4Context {
  override val contextFunctionName = "setClass"
}