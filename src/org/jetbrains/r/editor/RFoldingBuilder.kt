// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.editor

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import org.jetbrains.r.parsing.RElementTypes
import org.jetbrains.r.psi.api.RBlockExpression
import org.jetbrains.r.psi.api.RForStatement
import org.jetbrains.r.psi.api.RFunctionExpression
import org.jetbrains.r.psi.api.RIfStatement
import java.util.*

/**
 * Defines how code folding should behave for R files
 * For details see http://www.jetbrains.org/intellij/sdk/docs/tutorials/custom_language_support/folding_builder.html
 */
class RFoldingBuilder : FoldingBuilder {
  override fun getPlaceholderText(node: ASTNode): String? {
    return when (node.elementType) {
      RElementTypes.R_BLOCK_EXPRESSION -> "{...}"
      RElementTypes.R_FUNCTION_EXPRESSION -> {
        val def = node.psi as RFunctionExpression
        val funargs = def.parameterList ?: return null
        return "function ${funargs.text} ..."
      }
      RElementTypes.R_IF_STATEMENT -> {
        val def = node.psi as RIfStatement
        return "if (${def.expressionList[0].text})} ..."
      }
      RElementTypes.R_ELSE -> {
        // RIfStatement def = (RIfStatement) node.getPsi();
        return "else { ... } "
      }
      RElementTypes.R_FOR_STATEMENT -> {
        val def = node.psi as RForStatement
        return "for (${def.expressionList[0].text} in ${def.expressionList[1].text}) ..."
      }
      else -> null
    }
  }

  override fun isCollapsedByDefault(node: ASTNode): Boolean = false

  override fun buildFoldRegions(node: ASTNode, document: Document): Array<FoldingDescriptor> {
    val descriptors: MutableList<FoldingDescriptor> = ArrayList()
    appendDescriptors(node, descriptors)
    return descriptors.toTypedArray()
  }

  private fun appendDescriptors(node: ASTNode, descriptors: MutableList<FoldingDescriptor>) {
    if (node.elementType === RElementTypes.R_BLOCK_EXPRESSION) {
      val blockExpr = node.psi as RBlockExpression
      val lbraceStart = blockExpr.textRange.startOffset
      val rbraceStart = blockExpr.textRange.endOffset
      descriptors.add(FoldingDescriptor(node, TextRange(lbraceStart, rbraceStart)))
    }
    var child = node.firstChildNode
    while (child != null) {
      appendDescriptors(child, descriptors)
      child = child.treeNext
    }
  }
}