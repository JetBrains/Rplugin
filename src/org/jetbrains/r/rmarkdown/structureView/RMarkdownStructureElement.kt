/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.rmarkdown.structureView

import com.intellij.ide.IdeBundle
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.LocationPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.ui.Queryable
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.PsiFileImpl
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownCodeFence
import org.intellij.plugins.markdown.structureView.MarkdownBasePresentation
import org.jetbrains.r.RLanguage
import org.jetbrains.r.psi.RStructureViewElement
import org.jetbrains.r.rmarkdown.RMarkdownPsiUtil
import org.jetbrains.r.rmarkdown.R_FENCE_ELEMENT_TYPE
import javax.swing.Icon

private val DUMMY_PRESENTATION = object : MarkdownBasePresentation() {
  override fun getPresentableText(): String? = null
}

class RMarkdownStructureElement internal constructor(element: PsiElement) : PsiTreeElementBase<PsiElement>(
  element), SortableTreeElement, LocationPresentation, Queryable {

  override fun getAlphaSortKey(): String = (element as? NavigationItem)?.name ?: ""

  override fun isSearchInLocationString(): Boolean = true

  override fun getPresentableText(): String? {
    element ?: return IdeBundle.message("node.structureview.invalid")
    return presentation.presentableText
  }

  override fun getLocationString(): String? = presentation.locationString

  override fun getPresentation(): ItemPresentation {
    val element = element
    if (element is PsiFileImpl) {
      return element.presentation ?: DUMMY_PRESENTATION
    }

    val itemPresent = (element as? NavigationItem)?.presentation
    if (itemPresent != null) {
      return if (element is MarkdownCodeFence) {
        val label = RMarkdownPsiUtil.getExecutableFenceLabel(element)
        object : ItemPresentation {
          override fun getLocationString(): String? = itemPresent.locationString

          override fun getIcon(unused: Boolean): Icon? = itemPresent.getIcon(unused)

          override fun getPresentableText(): String? = label
        }
      }
      else {
        itemPresent
      }
    }

    return DUMMY_PRESENTATION
  }

  private fun getFenceGuestRRoots(textRange: TextRange): List<ASTNode>? {
    val files: List<PsiFile> = element?.containingFile?.viewProvider?.allFiles ?: return null
    val fileRoot = files.find { it.language == RLanguage.INSTANCE } ?: return null
    return RMarkdownPsiUtil.findFenceRoots(fileRoot.node, textRange)
  }

  override fun getChildrenBase(): Collection<StructureViewTreeElement> {
    val result = ArrayList<StructureViewTreeElement>()

    val element: PsiElement? = this.element
    if (element is MarkdownCodeFence) {
      val rFence = element.node.findChildByType(R_FENCE_ELEMENT_TYPE)
      if (rFence != null) {
        val guestRoots = getFenceGuestRRoots(rFence.textRange)
        guestRoots?.forEach {
          it.psi.accept(RStructureViewElement.RStructureVisitor(result, rFence.textRange))
        }
        return result
      }
    }
    else {
      RMarkdownPsiUtil.processContainer(element) { result.add(RMarkdownStructureElement(it)) }
    }
    return result
  }

  override fun getLocationPrefix(): String = " "

  override fun getLocationSuffix(): String = ""

  override fun putInfo(info: MutableMap<in String, in String>) {
    info["text"] = presentableText ?: ""
    if (element !is PsiFileImpl) {
      info["location"] = locationString ?: ""
    }
  }
}
