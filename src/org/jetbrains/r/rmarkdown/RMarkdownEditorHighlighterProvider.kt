/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.lang.Language
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.ex.util.LayerDescriptor
import com.intellij.openapi.editor.ex.util.LayeredLexerEditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.fileTypes.EditorHighlighterProvider
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class RMarkdownEditorHighlighterProvider : EditorHighlighterProvider {
  override fun getEditorHighlighter(project: Project?,
                                    fileType: FileType,
                                    virtualFile: VirtualFile?,
                                    colors: EditorColorsScheme): EditorHighlighter {
    return RMarkdownTemplateEditorHighlighter(project, virtualFile, colors)
  }
}

private class RMarkdownTemplateEditorHighlighter(project: Project?,
                                                 virtualFile: VirtualFile?,
                                                 colors: EditorColorsScheme) :
  LayeredLexerEditorHighlighter(RMarkdownSyntaxHighlighter(), colors) {

  init {
    for (extension in RmdFenceProvider.EP_NAME.extensionList) {
      registerLayer(extension.fenceElementType, layerDescriptor(project, virtualFile, extension.fenceLanguage))
    }
  }

  private fun layerDescriptor(project: Project?,
                              virtualFile: VirtualFile?,
                              language: Language): LayerDescriptor {
    val fenceFactory = SyntaxHighlighterFactory.getLanguageFactory().forLanguage(language)
    val fenceHighlighter = fenceFactory.getSyntaxHighlighter(project, virtualFile)
    return LayerDescriptor(fenceHighlighter, "", null)
  }
}

private class RMarkdownSyntaxHighlighter : SyntaxHighlighterBase() {
  override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> = TextAttributesKey.EMPTY_ARRAY

  override fun getHighlightingLexer(): Lexer = PatchingLexer { tokenSequence -> RmdFenceProvider.matchHeader(tokenSequence)?.fenceElementType }
}
