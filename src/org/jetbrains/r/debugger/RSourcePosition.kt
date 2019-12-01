/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.debugger

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.impl.XSourcePositionImpl

data class RSourcePosition(val file: VirtualFile, val line: Int) {
  val xSourcePosition: XSourcePosition
    get() = XSourcePositionImpl.create(file, line)
}

