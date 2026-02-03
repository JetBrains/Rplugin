/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.lang.Language
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.templateLanguages.TemplateDataElementType
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.intellij.r.psi.rmarkdown.RMarkdownLanguage
import com.intellij.util.containers.FactoryMap
import org.intellij.plugins.markdown.lang.parser.MarkdownParserManager

class RMarkdownFileViewProviderFactory : FileViewProviderFactory {
  override fun createFileViewProvider(
    file: VirtualFile,
    language: Language?,
    manager: PsiManager,
    eventSystemEnabled: Boolean
  ): FileViewProvider = RMarkdownFileViewProvider(manager, file, eventSystemEnabled)
}

private val guestElementTypeMap: MutableMap<Language, TemplateDataElementType> = FactoryMap.create { RMarkdownTemplate(it) }

class RMarkdownFileViewProvider(
  manager: PsiManager,
  val file: VirtualFile,
  eventSystemEnabled: Boolean
) : MultiplePsiFilesPerDocumentFileViewProvider(manager, file, eventSystemEnabled), TemplateLanguageFileViewProvider {

  override fun cloneInner(fileCopy: VirtualFile): MultiplePsiFilesPerDocumentFileViewProvider {
    return RMarkdownFileViewProvider(manager, fileCopy, false)
  }

  override fun getLanguages(): Set<Language> {
    return setOf(baseLanguage) + RmdFenceProvider.EP_NAME.extensionList.map { it.fenceLanguage }.toSet()
  }

  override fun getBaseLanguage(): Language {
    return RMarkdownLanguage
  }

  override fun getTemplateDataLanguage(): Language {
    return RMarkdownLanguage
  }

  override fun createFile(lang: Language): PsiFile? {
    if (RmdFenceProvider.find { it.fenceLanguage == lang } == null) {
      return super.createFile(lang)?.apply {
        putUserData(MarkdownParserManager.FLAVOUR_DESCRIPTION, RMarkdownFlavourDescriptor)
      }
    }

    val elementType = guestElementTypeMap[lang]
    val parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(lang)!!
    return (parserDefinition.createFile(this) as PsiFileImpl).apply { contentElementType = elementType }
  }
}