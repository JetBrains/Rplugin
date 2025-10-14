/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.debugger

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.r.psi.RFileType
import com.intellij.r.psi.psi.RFileImpl
import com.intellij.testFramework.LightVirtualFile
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProviderBase

object RDebuggerEditorsProvider : XDebuggerEditorsProviderBase() {
  override fun createExpressionCodeFragment(project: Project, text: String, context: PsiElement?, isPhysical: Boolean): PsiFile {
    val name = "fragment" + RFileType.defaultExtension
    val viewProvider = PsiManagerEx.getInstanceEx(project).getFileManager().createFileViewProvider(
      LightVirtualFile(name, RFileType, text), isPhysical)
    return RFileImpl(viewProvider)
  }

  override fun getFileType(): FileType = RFileType
}