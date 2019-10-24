// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.breakpoints.XLineBreakpointTypeBase


class RLineBreakpointType : XLineBreakpointTypeBase(ID, TITLE, REditorsProvider()) {
  override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean {
    return RLineBreakpointUtils.canPutAt(project, file, line)
  }

  companion object {
    private const val ID = "the-r-line"
    private const val TITLE = "R Breakpoints"
  }
}
