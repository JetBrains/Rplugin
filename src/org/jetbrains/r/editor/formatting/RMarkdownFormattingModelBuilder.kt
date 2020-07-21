/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor.formatting

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.DocumentBasedFormattingModel
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.impl.source.tree.SharedImplUtil
import com.intellij.psi.tree.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.plugins.markdown.lang.MarkdownElementType
import org.jetbrains.r.rmarkdown.RMarkdownPsiUtil
import org.jetbrains.r.rmarkdown.RmdFenceProvider

private val MARKDOWN_CODE_FENCE: IElementType = MarkdownElementType.platformType(MarkdownElementTypes.CODE_FENCE)

class RMarkdownFormattingModelBuilder : FormattingModelBuilder {
  override fun createModel(formattingContext: FormattingContext): FormattingModel {
    val rmdFile = formattingContext.containingFile
    val files: List<PsiFile> = formattingContext.containingFile.viewProvider.allFiles
    val rmdRoot = rmdFile.node!!
    val settings = formattingContext.codeStyleSettings
    val context = TemplateContext(files, settings)
    val rootBlock = TemplateAbstractBlock(rmdRoot, context)
    return DocumentBasedFormattingModel(rootBlock, settings, rmdFile)
  }
}

class TemplateContext(val files: List<PsiFile>, val settings: CodeStyleSettings) {
  fun getFenceBlocks(textRange: TextRange, language: Language, mapper: (roots: List<ASTNode>) -> List<Block>): List<Block>? {
    return files.find { it.language == language }?.let { psiFile ->
      val roots = RMarkdownPsiUtil.findFenceRoots(psiFile.node, textRange) ?: return null
      mapper(roots)
    }
  }
}

private class TemplateAbstractBlock(node: ASTNode, private val context: TemplateContext) : AbstractBlock(node, null, null) {
  override fun getSpacing(child1: Block?, child2: Block): Spacing? = null

  override fun isLeaf(): Boolean = node.firstChildNode == null

  override fun getIndent(): Indent = Indent.getNoneIndent()

  override fun buildChildren(): List<Block> {

    if (node.elementType == MARKDOWN_CODE_FENCE) {
      return node.getChildren(null).map { child ->
        if (RMarkdownPsiUtil.isSpace(child)) return@map emptyList<Block>()

        RmdFenceProvider.find { child.elementType == it.fenceElementType }
          ?.getFormattingBlocks(context, child.textRange)
        ?: listOf(TemplateAbstractBlock(child, context))
      }.flatten()
    }

    return node.getChildren(null).mapNotNull { child ->
      if (RMarkdownPsiUtil.isSpace(child)) null else TemplateAbstractBlock(child, context)
    }
  }

  override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
    if (node.elementType == MARKDOWN_CODE_FENCE) {
      RmdFenceProvider.find { SharedImplUtil.getChildrenOfType(node, it.fenceElementType).isNotEmpty() }?.let {
        return it.getNewChildAttributes(newChildIndex)
      }
    }
    return ChildAttributes(Indent.getNoneIndent(), null)
  }
}
