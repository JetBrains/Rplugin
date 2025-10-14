/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.execution.console.ConsoleRootType
import com.intellij.ide.scratch.RootType
import com.intellij.r.psi.RBundle

class RConsoleRootType internal constructor() : ConsoleRootType("R", RBundle.message("content.root.r.console.display.name")) {
  companion object {

    val instance: RConsoleRootType
      get() = RootType.findByClass(RConsoleRootType::class.java)
  }
}
