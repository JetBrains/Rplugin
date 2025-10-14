// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.r.psi.RFileType
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProviderBase

internal class REditorsProvider : XDebuggerEditorsProviderBase() {
  override fun getFileType(): FileType = RFileType

  override fun createExpressionCodeFragment(project: Project, text: String, context: PsiElement?, isPhysical: Boolean): PsiFile {
    return RCodeFragment(project, FRAGMENT_NAME, text)
  }

  companion object {
    private const val FRAGMENT_NAME = "fragment.r"
  }
}
