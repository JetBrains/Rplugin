/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.lang.Language
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.jetbrains.python.PythonLanguage
import org.jetbrains.r.RLanguage

object RMarkdownFileViewProviderFactory : FileViewProviderFactory {
  override fun createFileViewProvider(
    file: VirtualFile,
    language: Language?,
    manager: PsiManager,
    eventSystemEnabled: Boolean
  ): FileViewProvider = RMarkdownFileViewProvider(manager, file, eventSystemEnabled)
}

class RMarkdownFileViewProvider(
  manager: PsiManager,
  val file: VirtualFile,
  eventSystemEnabled: Boolean
) : MultiplePsiFilesPerDocumentFileViewProvider(manager, file, eventSystemEnabled), TemplateLanguageFileViewProvider {

  override fun cloneInner(fileCopy: VirtualFile): MultiplePsiFilesPerDocumentFileViewProvider {
    return RMarkdownFileViewProvider(manager, fileCopy, false)
  }

  override fun getLanguages(): MutableSet<Language> {
    return mutableSetOf(baseLanguage, RLanguage.INSTANCE, PythonLanguage.INSTANCE)
  }

  override fun getBaseLanguage(): Language {
    return RMarkdownLanguage
  }

  override fun getTemplateDataLanguage(): Language {
    return RMarkdownLanguage
  }

  override fun createFile(lang: Language): PsiFile? {
    val elementType = when(lang) {
      RLanguage.INSTANCE -> R_TEMPLATE
      PythonLanguage.INSTANCE -> PYTHON_TEMPLATE
      else -> return super.createFile(lang)
    }
    val parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(lang)!!
    return (parserDefinition.createFile(this) as PsiFileImpl).apply { contentElementType = elementType }
  }
}