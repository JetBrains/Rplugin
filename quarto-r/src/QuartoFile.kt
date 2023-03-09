package com.intellij.quarto

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class QuartoFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, QuartoLanguage) {
  override fun getFileType() = QuartoFileType
}