/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor.formatting

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.tree.SharedImplUtil
import com.intellij.psi.tree.TokenSet
import com.intellij.util.containers.FactoryMap
import org.jetbrains.r.RLanguage
import org.jetbrains.r.parsing.RElementTypes
import org.jetbrains.r.parsing.RParserDefinition
import org.jetbrains.r.parsing.RTokenTypes
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.*

private val NON_INDENT_PARTS = TokenSet.create(
  RElementTypes.R_RPAR,
  RElementTypes.R_RBRACE,
  RElementTypes.R_RBRACKET,
  RElementTypes.R_RDBRACKET,
  RElementTypes.R_LPAR,
  RElementTypes.R_LBRACE,
  RElementTypes.R_LBRACKET,
  RElementTypes.R_LDBRACKET,
  RElementTypes.R_ELSE
)

val R_SPACE_TOKENS = TokenSet.create(
  TokenType.WHITE_SPACE,
  RTokenTypes.END_OF_LINE_COMMENT
)

class RFormattingContext(private val settings: CodeStyleSettings) {
  private val spacingBuilder = createSpacingBuilder(settings)

  private val childIndentAlignments: MutableMap<ASTNode, Alignment> = FactoryMap.create { Alignment.createAlignment() }

  /** Use argument list as anchor */
  private val assignInParametersAlignments: MutableMap<ASTNode, Alignment> = FactoryMap.create { Alignment.createAlignment(true) }

  /** Use first element in row as anchor */
  private val alignmentByAnchor: MutableMap<ASTNode, Alignment> = FactoryMap.create { Alignment.createAlignment(true) }

  fun computeAlignment(node: ASTNode): Alignment? {
    val common = settings.getCommonSettings(RLanguage.INSTANCE)
    val custom = settings.getCustomSettings(RCodeStyleSettings::class.java)

    val nodeParent = node.treeParent ?: return null

    val alignParameters = common.ALIGN_MULTILINE_PARAMETERS && nodeParent.elementType == RElementTypes.R_PARAMETER_LIST &&
                          (node.elementType == RElementTypes.R_PARAMETER ||
                           node.elementType == RElementTypes.R_COMMA ||
                           isCommentAtEmptyLine(node))

    val alignCallArguments = common.ALIGN_MULTILINE_PARAMETERS_IN_CALLS &&
                             nodeParent.elementType == RElementTypes.R_ARGUMENT_LIST &&
                             (node.firstChildNode != null || isCommentAtEmptyLine(node))
                             && !hasMultilineBlock(nodeParent)

    val alignSubscriptionArguments = common.ALIGN_MULTILINE_PARAMETERS_IN_CALLS && nodeParent.elementType == RElementTypes.R_SUBSCRIPTION_EXPRESSION &&
                             (node != nodeParent.firstChildNode && node.firstChildNode != null || isCommentAtEmptyLine(node))

    if (alignCallArguments || alignParameters || alignSubscriptionArguments) {
      return childIndentAlignments[nodeParent]
    }

    if (custom.ALIGN_COMMENTS &&
        node.elementType == RTokenTypes.END_OF_LINE_COMMENT &&
        nodeParent.elementType == RElementTypes.R_ARGUMENT_LIST &&
        (findPrevNonSpaceNode(node)?.elementType == RElementTypes.R_COMMA ||
         findNextMeaningSibling(node)?.elementType == RElementTypes.R_RPAR)
    ) {
      findFirstCommentAfterComma(nodeParent.firstChildNode)?.let {
        return alignmentByAnchor[it]
      }
    }

    val nodeGrandParent = nodeParent.treeParent ?: return null
    if (custom.ALIGN_ASSIGNMENT_OPERATORS &&
        node.elementType == RElementTypes.R_ASSIGN_OPERATOR &&
        (nodeParent.elementType == RElementTypes.R_ASSIGNMENT_STATEMENT || nodeParent.elementType == RElementTypes.R_NAMED_ARGUMENT)) {
      if (nodeGrandParent.elementType == RElementTypes.R_ARGUMENT_LIST) {
        return assignInParametersAlignments[nodeGrandParent]
      }
      if (!isFunctionDeclarationNode(nodeParent) && nodeGrandParent.elementType == RElementTypes.R_BLOCK_EXPRESSION ||
          nodeGrandParent.elementType == RParserDefinition.FILE) {
        val anchor = findFirstAssignmentInTable(nodeParent)
        return alignmentByAnchor[anchor]
      }
    }
    return null
  }

  private fun hasMultilineBlock(node: ASTNode): Boolean =
    SharedImplUtil.getChildrenOfType(node, RElementTypes.R_BLOCK_EXPRESSION).any { it.textContains('\n') }

  private fun isCommentAtEmptyLine(node: ASTNode) =
    node.elementType == RTokenTypes.END_OF_LINE_COMMENT &&
    node.treePrev?.let { RPsiUtil.isWhitespaceWithNL(it) } ?: false

  private fun findFirstCommentAfterComma(start: ASTNode?): ASTNode? {
    var current: ASTNode? = start
    while (current != null) {
      if (current.elementType == RElementTypes.R_COMMA) {
        current = current.treeNext
        while (current?.elementType == TokenType.WHITE_SPACE)
          current = current?.treeNext
        if (current?.elementType == RTokenTypes.END_OF_LINE_COMMENT) {
          return current
        }
      } else {
        current = current.treeNext
      }
    }
    return null
  }

  private fun findNextMeaningSibling(start: ASTNode): ASTNode? {
    var current: ASTNode? = start
    while (current != null) {
      if (!R_SPACE_TOKENS.contains(current.elementType)) {
        return current
      }
      current = current.treeNext
    }
    return null
  }

  private fun isFunctionDeclarationNode(nodeParent: ASTNode) =
    (nodeParent.psi as? RAssignmentStatement)?.isFunctionDeclaration ?: false

  private fun findFirstAssignmentInTable(start: ASTNode): ASTNode {
    var current: ASTNode? = start
    var answer: ASTNode = start
    while (current != null) {
      if (current.firstChildNode != null) {
        if (current.elementType == RElementTypes.R_ASSIGNMENT_STATEMENT && !isFunctionDeclarationNode(current)) {
          answer = current
        } else {
          return answer
        }
      }

      if (RPsiUtil.isWhitespaceWithNL(current)) {
        if (current.text.count { it == '\n' } > 1) {
          return answer
        }
      }
      current = current.treePrev
    }
    return answer
  }

  fun computeSpacing(parent: Block, child1: Block?, child2: Block): Spacing? {
    return computeComplexSpacing(child1, child2) ?: spacingBuilder.getSpacing(parent, child1, child2)
  }

  private fun computeComplexSpacing(child1: Block?,
                                    child2: Block): Spacing? {
    val node1 = (child1 as? RFormatterBlock)?.node ?: return null
    val node2 = (child2 as? RFormatterBlock)?.node ?: return null
    if (isBigFunctionDeclaration(node1) || (isBigFunctionDeclaration(node2) &&
                                            node1.elementType != RTokenTypes.END_OF_LINE_COMMENT &&
                                            node1.elementType != RTokenTypes.ROXYGEN_COMMENT)) {
      val common = settings.getCommonSettings(RLanguage.INSTANCE)
      return Spacing.createSpacing(0, 0, 2, false, common.KEEP_BLANK_LINES_IN_CODE)
    }
    return null
  }

  private fun isBigFunctionDeclaration(node1: ASTNode?) =
    (node1?.psi as? RAssignmentStatement)?.isFunctionDeclaration == true && node1.textContains('\n')

  fun computeNewChildIndent(node: ASTNode): Indent {
    return when {
      node.psi is RFile -> Indent.getNoneIndent()
      node.psi is RBlockExpression -> Indent.getNormalIndent()
      node.psi is RExpression -> Indent.getContinuationIndent()
      node.psi is RArgumentList -> Indent.getContinuationIndent()
      else -> Indent.getNoneIndent()
    }
  }

  fun computeWrap(node: ASTNode): Wrap {
    val wrapType: WrapType = if (isRightExprInOpChain(node.psi)) WrapType.ALWAYS else WrapType.NONE
    return Wrap.createWrap(wrapType, true)
  }

  fun computeBlockIndent(node: ASTNode): Indent? {
    val psi = node.psi
    val parent = psi.parent
    return when {
      parent is RFile -> Indent.getNoneIndent()
      node.elementType == RElementTypes.R_COMMA -> Indent.getContinuationIndent()
      NON_INDENT_PARTS.contains(node.elementType) -> Indent.getNoneIndent()
      psi is RParameter -> Indent.getContinuationIndent()
      psi is RBlockExpression -> Indent.getNoneIndent()
      psi is RArgumentList -> Indent.getNoneIndent()
      psi is RExpression && parent is RArgumentList -> Indent.getContinuationIndent()
      parent is RBlockExpression-> Indent.getIndent(Indent.Type.NORMAL, false, false)
      parent is RExpression && parent.firstChild != psi -> Indent.getContinuationWithoutFirstIndent()
      parent is RArgumentList && psi is PsiComment -> Indent.getContinuationIndent()
      else -> Indent.getNoneIndent()
    }
  }

  private fun isRightExprInOpChain(blockPsi: PsiElement): Boolean {
    val opExpr = blockPsi.parent as? ROperatorExpression ?: return false

    // is right expr
    if (!opExpr.isBinary || opExpr.rightExpr !== blockPsi) return false

    val chainRootExpr = getSameOpChainRoot(opExpr)

    // is actual chain to avoid wrapping of simple binary operator expressions
    val rootOperator = chainRootExpr.operator ?: return false
    val opCapture = buildOpExpCapture(rootOperator)
    // at least chain of 3
    return if (opCapture.withChild(opCapture).accepts(chainRootExpr)) chainRootExpr.text.length >= 50 else false

    // is long enough to justify wrapping
  }

  private fun buildOpExpCapture(rOperator: ROperator): PsiElementPattern.Capture<ROperatorExpression> {
    return PlatformPatterns.psiElement(ROperatorExpression::class.java).withChild(
      PlatformPatterns.psiElement(ROperator::class.java).withText(rOperator.text))
  }

  private fun getSameOpChainRoot(opExpression: ROperatorExpression): ROperatorExpression {
    var opExpr = opExpression
    while (true) {
      val opParent = opExpr.parent as? ROperatorExpression ?: return opExpr

      val exOperator = opExpr.operator ?: return opExpr
      if (opParent.operator?.text != exOperator.text) {
        return opExpr
      }
      opExpr = opParent
    }
  }
}

private fun createSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder {
  val common = settings.getCommonSettings(RLanguage.INSTANCE)
  val custom = settings.getCustomSettings(RCodeStyleSettings::class.java)

  return SpacingBuilder(settings, RLanguage.INSTANCE)
    // Comments
    .before(RTokenTypes.END_OF_LINE_COMMENT).spacing(1, Int.MAX_VALUE, 0, true, common.KEEP_BLANK_LINES_IN_CODE)

    // Unary operators
    .afterInside(RElementTypes.R_TILDE_OPERATOR, RElementTypes.R_UNARY_TILDE_EXPRESSION).spaceIf(common.SPACE_AROUND_UNARY_OPERATOR)
    .afterInside(RElementTypes.R_PLUSMINUS_OPERATOR, RElementTypes.R_UNARY_PLUSMINUS_EXPRESSION).spaceIf(common.SPACE_AROUND_UNARY_OPERATOR)
    .after(RElementTypes.R_NOT_OPERATOR).spaceIf(common.SPACE_AROUND_UNARY_OPERATOR)

    // Binary operators
    .around(RElementTypes.R_ASSIGN_OPERATOR).spaceIf(common.SPACE_AROUND_ASSIGNMENT_OPERATORS)
    .aroundInside(RElementTypes.R_EQ, RElementTypes.R_PARAMETER).spaceIf(common.SPACE_AROUND_ASSIGNMENT_OPERATORS)

    .around(RElementTypes.R_TILDE_OPERATOR).spaceIf(custom.SPACE_AROUND_BINARY_TILDE_OPERATOR)
    .around(RElementTypes.R_COMPARE_OPERATOR).spaceIf(common.SPACE_AROUND_RELATIONAL_OPERATORS)
    .around(RElementTypes.R_OR_OPERATOR).spaceIf(custom.SPACE_AROUND_DISJUNCTION_OPERATORS)
    .around(RElementTypes.R_AND_OPERATOR).spaceIf(custom.SPACE_AROUND_CONJUNCTION_OPERATORS)
    .around(RElementTypes.R_PLUSMINUS_OPERATOR).spaceIf(common.SPACE_AROUND_ADDITIVE_OPERATORS)
    .around(RElementTypes.R_MULDIV_OPERATOR).spaceIf(common.SPACE_AROUND_MULTIPLICATIVE_OPERATORS)
    .around(RElementTypes.R_INFIX_OPERATOR).spaceIf(custom.SPACE_AROUND_INFIX_OPERATOR)
    .around(RElementTypes.R_COLON_OPERATOR).spaceIf(custom.SPACE_AROUND_COLON_OPERATOR)
    .around(RElementTypes.R_EXP_OPERATOR).spaceIf(custom.SPACE_AROUND_EXPONENTIATION_OPERATOR)
    .around(RElementTypes.R_LIST_SUBSET_OPERATOR).spaceIf(custom.SPACE_AROUND_SUBSET_OPERATOR)
    .around(RElementTypes.R_AT_OPERATOR).spaceIf(custom.SPACE_AROUND_AT_OPERATOR)

    // Leave this hardcoded until request (forever)
    .around(RElementTypes.R_DOUBLECOLON).spaces(0)
    .around(RElementTypes.R_TRIPLECOLON).spaces(0)

    // Parentheses group
    .beforeInside(RElementTypes.R_ARGUMENT_LIST, RElementTypes.R_CALL_EXPRESSION).spaceIf(common.SPACE_BEFORE_METHOD_CALL_PARENTHESES)
    .beforeInside(RElementTypes.R_PARAMETER_LIST, RElementTypes.R_FUNCTION_EXPRESSION).spaceIf(common.SPACE_BEFORE_METHOD_PARENTHESES)
    .between(RElementTypes.R_IF, RElementTypes.R_LPAR).spaceIf(common.SPACE_BEFORE_IF_PARENTHESES)
    .between(RElementTypes.R_WHILE, RElementTypes.R_LPAR).spaceIf(common.SPACE_BEFORE_WHILE_PARENTHESES)
    .between(RElementTypes.R_FOR, RElementTypes.R_LPAR).spaceIf(common.SPACE_BEFORE_FOR_PARENTHESES)

    // Bracket group
    .beforeInside(RElementTypes.R_BLOCK_EXPRESSION, RElementTypes.R_FUNCTION_EXPRESSION).spaceIf(common.SPACE_BEFORE_METHOD_LBRACE)
    .beforeInside(RElementTypes.R_BLOCK_EXPRESSION, RElementTypes.R_IF_STATEMENT).spaceIf(common.SPACE_BEFORE_IF_LBRACE)
    .beforeInside(RElementTypes.R_BLOCK_EXPRESSION, RElementTypes.R_WHILE_STATEMENT).spaceIf(common.SPACE_BEFORE_WHILE_LBRACE)
    .beforeInside(RElementTypes.R_BLOCK_EXPRESSION, RElementTypes.R_FOR_STATEMENT).spaceIf(common.SPACE_BEFORE_FOR_LBRACE)
    .beforeInside(RElementTypes.R_BLOCK_EXPRESSION, RElementTypes.R_REPEAT_STATEMENT).spaceIf(custom.SPACE_BEFORE_REPEAT_LBRACE)
    .before(RElementTypes.R_LBRACKET).spaceIf(custom.SPACE_BEFORE_LEFT_BRACKET)

    // Within group
    .after(RElementTypes.R_LBRACE).spaceIf(common.SPACE_WITHIN_BRACES)
    .before(RElementTypes.R_RBRACE).spaceIf(common.SPACE_WITHIN_BRACES)

    .after(RElementTypes.R_LBRACKET).spaceIf(common.SPACE_WITHIN_BRACKETS)
    .before(RElementTypes.R_RBRACKET).spaceIf(common.SPACE_WITHIN_BRACKETS)

    .after(RElementTypes.R_LPAR).spaceIf(common.SPACE_WITHIN_PARENTHESES)
    .before(RElementTypes.R_RPAR).spaceIf(common.SPACE_WITHIN_PARENTHESES)

    // Other
    .after(RElementTypes.R_COMMA).spaceIf(common.SPACE_AFTER_COMMA)
    .before(RElementTypes.R_COMMA).spaceIf(common.SPACE_BEFORE_COMMA)

    .after(RElementTypes.R_SEMI).spaces(1)

    .aroundInside(TokenSet.ANY, TokenSet.create(RParserDefinition.FILE, RElementTypes.R_BLOCK_EXPRESSION))
    .spacing(0, 0, 0, true, common.KEEP_BLANK_LINES_IN_CODE)
}

fun findPrevNonSpaceNode(start: ASTNode): ASTNode? {
  var current: ASTNode? = start.treePrev
  while (current != null) {
    if (current.elementType != TokenType.WHITE_SPACE) {
      return current
    }
    current = current.treePrev
  }
  return null
}
