/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.lang.Language
import com.intellij.lexer.Lexer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.templateLanguages.TemplateDataElementType
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.OuterLanguageElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.accept
import org.intellij.markdown.ast.visitors.RecursiveVisitor
import org.intellij.markdown.parser.MarkdownParser
import java.util.Locale
import org.intellij.markdown.IElementType as MarkdownIElementType

val INNER_ELEMENT = IElementType("INNER", RMarkdownLanguage)
val OUTER_ELEMENT = OuterLanguageElementType("OUTER", RMarkdownLanguage)


class RMarkdownTemplate(private val templateLanguage: Language)
  : TemplateDataElementType(
  "RMARKDOWN_TEMPLATE_${templateLanguage.displayName}",
  RMarkdownLanguage,
  INNER_ELEMENT,
  OUTER_ELEMENT) {

  override fun getTemplateFileLanguage(viewProvider: TemplateLanguageFileViewProvider?): Language {
    return templateLanguage
  }

  override fun createTemplateFile(psiFile: PsiFile,
                                  templateLanguage: Language,
                                  sourceCode: CharSequence,
                                  viewProvider: TemplateLanguageFileViewProvider?,
                                  rangeCollector: RangeCollector): PsiFile {
    val templateSourceCode = StringBuilder()
    val innerRanges = mutableListOf<TextRange>()
    val root = MarkdownParser(RMarkdownFlavourDescriptor).parse(MarkdownIElementType("ROOT"), sourceCode.toString())

    var depth = 0
    root.accept(object: RecursiveVisitor() {
      override fun visitNode(node: ASTNode) {
        if (node.type == MarkdownElementTypes.CODE_FENCE && depth == 1) {
          val children = node.children
          children.indexOfFirst { it.type == MarkdownTokenTypes.FENCE_LANG}.takeIf { it != -1 }?.let { languageIndex ->
            if (getLanguage(children[languageIndex], sourceCode) != templateLanguage.id.lowercase(Locale.getDefault())) return@let
            /*
             * The Code Fence AST has the following structure:
             *   CodeFence --- ```     (0)
             *              |- LANG    (1)
             *              |- NL      (2)
             *              |- CONTENT (3)
             *              |- NL      (4)
             *              ...
             *              |- CONTENT (size - 3)
             *              | - NL     (size - 2)
             *              \ - ```    (size - 1)
             *
             * So, we want to copy from the LANG index (which should be 1) to size - 2
             * It means that after the LANG index we need at least 4 elements: NL CONTENT NL ```
             */
            if (languageIndex + 3 >= children.size) return@let
            val range = TextRange(children[languageIndex + 1].startOffset, children[children.size - 2].endOffset)
            innerRanges.add(range)
            templateSourceCode.append(sourceCode.subSequence(range.startOffset, range.endOffset))
          }
        }
        depth++
        super.visitNode(node)
        depth--
      }
    })
    var lhs = 0
    innerRanges.forEach { textRange ->
      if (lhs < textRange.startOffset) {
        rangeCollector.addOuterRange(TextRange(lhs, textRange.startOffset))
      }
      lhs = textRange.endOffset
    }
    if (lhs < sourceCode.length) {
      rangeCollector.addOuterRange(TextRange(lhs, sourceCode.length))
    }

    return createPsiFileFromSource(templateLanguage, templateSourceCode, psiFile.manager)
  }

  override fun createTemplateText(sourceCode: CharSequence, baseLexer: Lexer, rangeCollector: RangeCollector): CharSequence {
    throw NotImplementedError("This method shouldn't be called")
  }
}

fun getLanguage(fenceLanguage: ASTNode, sourceCode: CharSequence): String? {
  val text = sourceCode.substring(fenceLanguage.startOffset, fenceLanguage.endOffset).lowercase(Locale.getDefault())
  return RMarkdownPsiUtil.getExecutableFenceLanguage(text)
}
