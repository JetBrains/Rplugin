package org.jetbrains.r.quarto

import com.intellij.lang.Language
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.templateLanguages.TemplateDataElementType
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.intellij.quarto.QmdFenceProvider
import com.intellij.quarto.QuartoLanguage
import com.intellij.util.containers.FactoryMap
import org.intellij.plugins.markdown.lang.parser.MarkdownParserManager
import org.jetbrains.r.rmarkdown.RMarkdownFlavourDescriptor
import org.jetbrains.r.rmarkdown.RMarkdownTemplate

private class QuartoFileViewProviderFactory : FileViewProviderFactory {
  override fun createFileViewProvider(
    file: VirtualFile,
    language: Language?,
    manager: PsiManager,
    eventSystemEnabled: Boolean
  ): FileViewProvider = QuartoFileViewProvider(manager, file, eventSystemEnabled)
}

private val guestElementTypeMap: MutableMap<Language, TemplateDataElementType> = FactoryMap.create { RMarkdownTemplate(it) }

private class QuartoFileViewProvider(
  manager: PsiManager,
  val file: VirtualFile,
  eventSystemEnabled: Boolean
) : MultiplePsiFilesPerDocumentFileViewProvider(manager, file, eventSystemEnabled), TemplateLanguageFileViewProvider {

  override fun cloneInner(fileCopy: VirtualFile): MultiplePsiFilesPerDocumentFileViewProvider {
    return QuartoFileViewProvider(manager, fileCopy, false)
  }

  override fun getLanguages(): Set<Language> {
    return setOf(baseLanguage) + QmdFenceProvider.EP_NAME.extensionList.map { it.fenceLanguage }.toSet()
  }

  override fun getBaseLanguage(): Language {
    return QuartoLanguage
  }

  override fun getTemplateDataLanguage(): Language {
    return QuartoLanguage
  }

  override fun createFile(lang: Language): PsiFile? {
    if (QmdFenceProvider.find { it.fenceLanguage == lang } == null) {
      return super.createFile(lang)?.apply {
        putUserData(MarkdownParserManager.FLAVOUR_DESCRIPTION, RMarkdownFlavourDescriptor)
      }
    }

    val elementType = guestElementTypeMap[lang]
    val parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(lang)!!
    return (parserDefinition.createFile(this) as PsiFileImpl).apply { contentElementType = elementType }
  }
}