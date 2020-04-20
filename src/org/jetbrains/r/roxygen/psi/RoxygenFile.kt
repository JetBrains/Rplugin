/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.jetbrains.r.roxygen.RoxygenFileType
import org.jetbrains.r.roxygen.RoxygenLanguage

class RoxygenFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, RoxygenLanguage.INSTANCE) {

  override fun getFileType(): FileType = RoxygenFileType

  override fun toString(): String = "Roxygen File"
}