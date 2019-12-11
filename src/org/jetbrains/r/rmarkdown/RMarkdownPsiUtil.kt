/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.formatter.FormatterUtil
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiUtilCore
import org.intellij.plugins.markdown.lang.MarkdownTokenTypeSets.*
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.intellij.plugins.markdown.lang.psi.MarkdownPsiElement
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownCodeFenceImpl
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownFile
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownHeaderImpl
import org.jetbrains.r.parsing.RElementTypes
import java.util.regex.Matcher
import java.util.regex.Pattern

object RMarkdownPsiUtil {
  val PRESENTABLE_TYPES = HEADERS
  val TRANSPARENT_CONTAINERS = TokenSet.create(MARKDOWN_FILE, UNORDERED_LIST, ORDERED_LIST, LIST_ITEM, BLOCK_QUOTE)

  val executableFenceLabelPattern: Pattern = Pattern.compile("\\{(\\w+)([^,]*)(,.*)?}", Pattern.DOTALL)

  private val HEADER_ORDER = listOf(
    TokenSet.create(MARKDOWN_FILE_ELEMENT_TYPE),
    HEADER_LEVEL_1_SET,
    HEADER_LEVEL_2_SET,
    HEADER_LEVEL_3_SET,
    HEADER_LEVEL_4_SET,
    HEADER_LEVEL_5_SET,
    HEADER_LEVEL_6_SET)

  fun isSpace(node: ASTNode): Boolean {
    return FormatterUtil.isWhitespaceOrEmpty(node) || node.elementType == RElementTypes.R_NL || node.elementType == MARKDOWN_EOL
  }

  fun getExecutableFenceLanguage(fullFenceHeader: CharSequence): String? {
    val matcher = executableFenceLabelPattern.matcher(fullFenceHeader)
    if (matcher.matches()) {
      return matcher.group(1).toLowerCase()
    }
    return null
  }

  /** Find top-level AST nodes (from guest language) inside guest fence */
  fun findFenceRoots(node: ASTNode, fenceRange: TextRange): List<ASTNode>? {
    if (node.textRange == fenceRange) {
      return listOf(node)
    }
    var child: ASTNode? = node.firstChildNode
    while (child != null) {
      when {
        child.textRange.contains(fenceRange) -> return findFenceRoots(child, fenceRange)

        fenceRange.startOffset == child.textRange.startOffset || (isSpace(child) && child.textRange.contains(fenceRange.startOffset)) -> {
          val result = ArrayList<ASTNode>()
          while (child != null) {
            if (!isSpace(child)) {
              result.add(child)
            }
            if (fenceRange.endOffset == child.textRange.endOffset || (isSpace(child) && child.textRange.contains(fenceRange.endOffset))) {
              return if (result.isNotEmpty()) result else null
            }
            if (child.textRange.contains(fenceRange.endOffset)) {
              return null
            }
            child = child.treeNext
          }
          return null
        }

        child.textRange.contains(fenceRange.startOffset) -> return null

        else -> child = child.treeNext
      }
    }
    return null
  }

  /**
   * @return There are 3 variants:
   *          1) found label name or
   *          2) empty string if the fence is unnamed or
   *          3) `null` if it is not executable fence at all
   */
  fun getExecutableFenceLabel(fence: MarkdownCodeFenceImpl): String {
    val label = getExecutableFenceLabelInt(fence) ?: return ""
    if (label == "") {
      val unnamedFences = getOrCalculateUnnamedExecutableFences(fence.containingFile)
      val number = unnamedFences[fence] ?: return ""
      return "unnamed-chunk-${number}"
    }
    else {
      return label
    }
  }

  /*
   * nextHeaderConsumer 'null' means reaching EOF
   */
  fun processContainer(myElement: PsiElement?,
                       consumer: (PsiElement) -> Any) {
    if (myElement == null) return

    val structureContainer = if (myElement is MarkdownFile)
      myElement.firstChild
    else
      getParentOfType(myElement, TRANSPARENT_CONTAINERS)

    structureContainer ?: return

    val currentHeader = if (isFitElement(myElement)) myElement as MarkdownPsiElement? else null
    processContainer(structureContainer, currentHeader, currentHeader, consumer)
  }

  private fun processContainer(container: PsiElement,
                               sameLevelRestriction: PsiElement?,
                               from: MarkdownPsiElement?,
                               resultConsumer: (PsiElement) -> Any) {
    var nextSibling: PsiElement? = if (from == null) container.firstChild else from.nextSibling
    var maxContentLevel: PsiElement? = null
    while (nextSibling != null) {
      if (TRANSPARENT_CONTAINERS.contains(PsiUtilCore.getElementType(nextSibling)) && maxContentLevel == null) {
        processContainer(nextSibling, null, null, resultConsumer)
      }
      else if (container.parent is MarkdownFile && sameLevelRestriction == null && from == null && isExeRFence(nextSibling)) {
        resultConsumer(nextSibling)
      }
      else if (isFitElement(nextSibling)) {
        if (sameLevelRestriction != null && isSameLevelOrHigher(nextSibling, sameLevelRestriction)) {
          break
        }

        if (maxContentLevel == null || isSameLevelOrHigher(nextSibling, maxContentLevel)) {
          maxContentLevel = nextSibling

          val type = nextSibling.node.elementType
          if (PRESENTABLE_TYPES.contains(type)) {
            resultConsumer(nextSibling)
          }
        }
      }

      nextSibling = nextSibling.nextSibling
    }
  }

  private fun isSameLevelOrHigher(psiA: PsiElement, psiB: PsiElement): Boolean {
    val typeA = psiA.node.elementType
    val typeB = psiB.node.elementType

    return headerLevel(typeA) <= headerLevel(typeB)
  }

  private fun headerLevel(curLevelType: IElementType): Int {
    for (i in HEADER_ORDER.indices) {
      if (HEADER_ORDER[i].contains(curLevelType)) {
        return i
      }
    }

    // not a header so return lowest level
    return Integer.MAX_VALUE
  }

  private fun getParentOfType(myElement: PsiElement, types: TokenSet): PsiElement? {
    val parentNode = TreeUtil.findParent(myElement.node, types)
    return parentNode?.psi
  }

  private fun isFitElement(element: PsiElement?): Boolean {
    return element is MarkdownHeaderImpl // || isExeRFence(element)
  }

  private fun isExeRFence(element: PsiElement?) = element is MarkdownCodeFenceImpl && getExecutableFenceLabelInt(element) != null

  /**
   * @return There are 3 variants:
   *          1) found label name or
   *          2) empty string if the fence is unnamed or
   *          3) `null` if it is not executable fence at all
   */
  private fun getExecutableFenceLabelInt(element: PsiElement): String? {
    val fenceLang = element.node.findChildByType(MarkdownTokenTypes.FENCE_LANG)?.text ?: return null
    val m: Matcher = executableFenceLabelPattern.matcher(fenceLang)
    return if (m.matches()) m.group(2).trim() else ""
  }

  private fun getOrCalculateUnnamedExecutableFences(file: PsiFile): HashMap<PsiElement, Int> {
    return CachedValuesManager.getCachedValue(file) {
      CachedValueProvider.Result(calculateUnnamedExecutableFences(file), file)
    }
  }

  private fun calculateUnnamedExecutableFences(file: PsiFile): HashMap<PsiElement, Int> {
    val result = HashMap<PsiElement, Int>()
    val rmdDoc = file.firstChild
    var id = 1
    for (child in rmdDoc.children) {
      if (child is MarkdownCodeFenceImpl && getExecutableFenceLabelInt(child) == "") {
        result[child] = id++
      }
    }
    return result
  }
}
