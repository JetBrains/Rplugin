/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor.formatting

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.DocumentBasedFormattingModel
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.impl.source.tree.SharedImplUtil
import com.intellij.psi.tree.IElementType
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.formatter.PyBlock
import com.jetbrains.python.formatter.PyBlockContext
import com.jetbrains.python.formatter.PythonFormattingModelBuilder
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.plugins.markdown.lang.MarkdownElementType
import org.jetbrains.r.RLanguage
import org.jetbrains.r.rmarkdown.PYTHON_FENCE_ELEMENT_TYPE
import org.jetbrains.r.rmarkdown.RMarkdownPsiUtil
import org.jetbrains.r.rmarkdown.R_FENCE_ELEMENT_TYPE

private val MARKDOWN_CODE_FENCE: IElementType = MarkdownElementType.platformType(MarkdownElementTypes.CODE_FENCE)

class RMarkdownFormattingModelBuilder : FormattingModelBuilder {
  override fun createModel(element: PsiElement, settings: CodeStyleSettings): FormattingModel {
    val rmdFile = element.containingFile!!
    val files: List<PsiFile> = element.containingFile.viewProvider.allFiles
    val rmdRoot = rmdFile.node!!
    val context = TemplateContext(files, settings)
    val rootBlock = TemplateAbstractBlock(rmdRoot, context)
    return DocumentBasedFormattingModel(rootBlock, settings, rmdFile)
  }
}

private class TemplateContext(val files: List<PsiFile>, val settings: CodeStyleSettings) {
  fun getFenceBlocks(textRange: TextRange, language: Language, mapper: (roots: List<ASTNode>) -> List<Block>): List<Block>? {
    return files.find { it.language == language }?.let { psiFile ->
      val roots = RMarkdownPsiUtil.findFenceRoots(psiFile.node, textRange) ?: return null
      mapper(roots)
    }
  }

  fun getRBlocks(textRange: TextRange): List<Block>? {
    return getFenceBlocks(textRange, RLanguage.INSTANCE) { roots ->
      val ctx = RFormattingContext(settings)
      roots.map { node -> RFormatterBlock(ctx, node) }
    }
  }

  fun getPythonBlocks(textRange: TextRange): List<Block>? {
    return getFenceBlocks(textRange, PythonLanguage.INSTANCE) { roots ->
      val context = PyBlockContext(settings, getPythonSpacingBuilder(settings), FormattingMode.REFORMAT)
      roots.map { node -> PyBlock(null, node, null, Indent.getNoneIndent(), null, context) }
    }
  }
}

private class TemplateAbstractBlock(node: ASTNode, private val context: TemplateContext) : AbstractBlock(node, null, null) {
  override fun getSpacing(child1: Block?, child2: Block): Spacing? = null

  override fun isLeaf(): Boolean = node.firstChildNode == null

  override fun getIndent(): Indent = Indent.getNoneIndent()

  override fun buildChildren(): List<Block> {
    val childSingleton: (node: ASTNode) -> List<Block> = { child -> listOf(TemplateAbstractBlock(child, context)) }

    if (node.elementType == MARKDOWN_CODE_FENCE) {
      return node.getChildren(null).map { child ->
        when {
          RMarkdownPsiUtil.isSpace(child) -> emptyList()
          child.elementType == R_FENCE_ELEMENT_TYPE -> context.getRBlocks(child.textRange) ?: childSingleton(child)
          child.elementType == PYTHON_FENCE_ELEMENT_TYPE -> context.getPythonBlocks(child.textRange) ?: childSingleton(child)
          else -> childSingleton(child)
        }
      }.flatten()
    }

    return node.getChildren(null).mapNotNull { child ->
      if (RMarkdownPsiUtil.isSpace(child)) null else TemplateAbstractBlock(child, context)
    }
  }

  override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
    return if (node.elementType == MARKDOWN_CODE_FENCE && SharedImplUtil.getChildrenOfType(node, PYTHON_FENCE_ELEMENT_TYPE).isNotEmpty()) {
      ChildAttributes.DELEGATE_TO_PREV_CHILD
    }
    else {
      ChildAttributes(Indent.getNoneIndent(), null)
    }
  }
}


private fun getPythonSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder {
  // A simple hack: PythonFormattingModelBuilder#createSpacingBuilder can be static method but now it is non-static and protected
  return object : PythonFormattingModelBuilder() {
    fun extractProtectedSpacingBuilder(): SpacingBuilder {
      return createSpacingBuilder(settings)
    }
  }.extractProtectedSpacingBuilder()
}
