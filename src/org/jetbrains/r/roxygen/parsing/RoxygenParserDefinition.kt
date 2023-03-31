/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen.parsing

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.annotations.NotNull
import org.jetbrains.r.roxygen.RoxygenLanguage
import org.jetbrains.r.roxygen.lexer.RoxygenLexer
import org.jetbrains.r.roxygen.psi.RoxygenFile

class RoxygenParserDefinition : ParserDefinition {

  override fun createLexer(project: Project): Lexer = RoxygenLexer()

  override fun getWhitespaceTokens(): TokenSet = TokenSet.EMPTY

  override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

  override fun getStringLiteralElements(): TokenSet = RoxygenTokenSets.STRINGS

  override fun createParser(project: Project): PsiParser = RoxygenParser()

  override fun getFileNodeType(): IFileElementType = FILE

  override fun createFile(viewProvider: @NotNull FileViewProvider): @NotNull PsiFile = RoxygenFile(viewProvider)

  override fun createElement(node: ASTNode): PsiElement = RoxygenElementTypes.Factory.createElement(node)

  companion object {
    val FILE: IFileElementType = IFileElementType(RoxygenLanguage.INSTANCE)
  }
}

object RoxygenTokenSets {
  // RoxygenElementTypes.ROXYGEN_TEXT cannot be a comment, because of Parser specific and
  // special events processing procedure for comment-in-comment. To support find usages in
  // comments and strings it becomes a string.
  val STRINGS = TokenSet.create(RoxygenElementTypes.ROXYGEN_TEXT)
}