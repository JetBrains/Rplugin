/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class RMarkdownFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, RMarkdownLanguage) {
  override fun getFileType() = RMarkdownFileType
}