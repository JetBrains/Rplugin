/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.psi.util.PsiTreeUtil.findChildOfAnyType
import com.intellij.r.psi.rmarkdown.RMarkdownFileType
import com.jetbrains.python.inspections.PyInspectionExtension
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFile

class RMarkdownPyInspectionExtension : PyInspectionExtension() {
  override fun ignoreInterpreterWarnings(file: PyFile) =
    file.virtualFile?.let { FileTypeRegistry.getInstance().isFileOfType(it, RMarkdownFileType) && findChildOfAnyType(file, PyElement::class.java) == null } ?: false
}