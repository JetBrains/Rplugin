/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.r.psi.rmarkdown.RMarkdownLanguage
import org.intellij.plugins.markdown.lang.parser.MarkdownParserAdapter
import org.intellij.plugins.markdown.lang.parser.MarkdownParserDefinition

class RMarkdownParserDefinition : MarkdownParserDefinition() {
  override fun getFileNodeType(): IFileElementType {
    return RMarkdownFileElementType
  }

  override fun createLexer(project: Project?): Lexer {
    return PatchingLexer { tokenSequence -> RmdFenceProvider.matchHeader(tokenSequence)?.fenceElementType }
  }

  override fun createParser(project: Project?): PsiParser {
    return MarkdownParserAdapter(RMarkdownFlavourDescriptor)
  }
}

private val RMarkdownFileElementType: IStubFileElementType<*> = IStubFileElementType<PsiFileStub<PsiFile>>("R Markdown", RMarkdownLanguage)
