// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.psi.PsiFile
import org.jetbrains.r.lsp.RLspStatus

abstract class RInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = !RLspStatus.isLspRunning(file.project)
}
