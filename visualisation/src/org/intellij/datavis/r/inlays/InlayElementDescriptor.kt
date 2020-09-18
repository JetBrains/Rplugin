/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.intellij.datavis.r.inlays.components.InlayProgressStatus
import java.util.concurrent.Future
import javax.swing.Icon

data class InlayOutput(val data: String,
                       val type: String,
                       var progressStatus: InlayProgressStatus? = null,
                       val title: String? = null,
                       val preview: Icon? = null,
                       val preferredWidth: Int = 0)

interface InlayElementDescriptor {
  /**
   * psi arguments of the interface methods belong to the [psiFile]
   */
  val psiFile: PsiFile

  /**
   * toolbar inlays, null if unsupported
   */
  @JvmDefault
  val toolbars: InlayToolbarElementDescriptor?
    get() = null

  /**
   * returns true if the [psi] could have inlay outputs
   */
  fun isInlayElement(psi: PsiElement): Boolean

  /**
   * @return the output for the [inlayElement], empty list if there is no outputs for the [inlayElement] and null if descriptor doesn't
   * store inlay outputs
   * @param inlayElement `isInlayElement(inlayElement)` is true
   */
  @JvmDefault
  fun getInlayOutputs(inlayElement: PsiElement): List<InlayOutput>? = null

  /**
   * the method is called when inlay output of [psi] is intentionally removed by a user.
   */
  @JvmDefault
  fun cleanup(psi: PsiElement): Future<Void>? = null

  /**
   * Returns offset in the document, to which an output inlay should be appended.
   */
  @JvmDefault
  fun getInlayOffset(psiElement: PsiElement): Int =
    // By default returns the offset to the last non-whitespace character in psiCell text.
    psiElement.textRange.endOffset - 1

  @JvmDefault
  fun shouldUpdateInlays(event: DocumentEvent): Boolean =
    event.oldFragment.contains("\n") || event.newFragment.contains("\n")
}

interface InlayToolbarElementDescriptor {
  /**
   * @return true if the [psi] have inlay toolbar actions
   */
  fun isToolbarActionElement(psi: PsiElement): Boolean

  /**
   * @return actions for an inlay toolbar
   * @param toolbarElement `isToolbarActionElement(toolbarElement)` is true
   */
  fun getToolbarActions(toolbarElement: PsiElement): ActionGroup?

  /**
   * Callback for updating toolbar elements highlighting.
   * The method is called when the highlighting of [toolbarElements] should be repainted.
   */
  fun onUpdateHighlighting(toolbarElements: Collection<PsiElement>)
}

interface InlayDescriptorProvider {

  fun getInlayDescriptor(editor: Editor): InlayElementDescriptor?

  companion object {
    val EP = ExtensionPointName.create<InlayDescriptorProvider>("com.intellij.datavis.inlays.inlayDescriptorProvider")
  }
}