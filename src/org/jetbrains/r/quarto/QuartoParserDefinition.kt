package org.jetbrains.r.quarto

import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.IStubFileElementType
import org.intellij.plugins.markdown.lang.parser.MarkdownParserAdapter
import org.intellij.plugins.markdown.lang.parser.MarkdownParserDefinition
import org.jetbrains.r.rmarkdown.PatchingLexer
import org.jetbrains.r.rmarkdown.RMarkdownFlavourDescriptor

class QuartoParserDefinition : MarkdownParserDefinition() {
  override fun getFileNodeType(): IFileElementType {
    return QuartoFileElementType
  }

  override fun createLexer(project: Project?): Lexer {
    return PatchingLexer { tokenSequence -> QmdFenceProvider.matchHeader(tokenSequence)?.fenceElementType }
  }

  override fun createParser(project: Project?): PsiParser {
    return MarkdownParserAdapter(RMarkdownFlavourDescriptor)
  }
}

private val QuartoFileElementType: IStubFileElementType<*> = IStubFileElementType<PsiFileStub<PsiFile>>("Quarto", QuartoLanguage)
