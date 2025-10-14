// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.r.psi

import com.intellij.lang.ASTNode
import com.intellij.lang.TokenSeparatorGenerator
import com.intellij.psi.TokenType
import com.intellij.psi.impl.source.tree.Factory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.psi.api.RExpression

class RTokenSeparatorGenerator : TokenSeparatorGenerator {
  override fun generateWhitespaceBetweenTokens(left: ASTNode?, right: ASTNode): ASTNode? {
    if (left?.elementType == TokenType.WHITE_SPACE || right.elementType == TokenType.WHITE_SPACE) {
      return null
    }

    if (left?.psi?.isValid == true && right.psi.isValid) {
      val commonParent = PsiTreeUtil.findCommonParent(left.psi, right.psi) ?: return null
      val leftPrevAncestor = PsiTreeUtil.findPrevParent(commonParent, left.psi)
      val rightPrevAncestor = PsiTreeUtil.findPrevParent(commonParent, right.psi)
      if (leftPrevAncestor is RExpression && rightPrevAncestor is RExpression) {
        return Factory.createSingleLeafElement(TokenType.WHITE_SPACE, "\n", 0, 1, null, right.treeParent.psi.manager)
      }
    }
    return null
  }
}
