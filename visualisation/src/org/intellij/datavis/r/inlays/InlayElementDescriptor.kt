/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.util.concurrent.Future
import javax.swing.Icon

data class InlayOutput(val data: String,
                       val type: String,
                       val title: String? = null,
                       val preview: Icon? = null,
                       val preferredWidth: Int = 0)

interface InlayElementDescriptor {

  val psiFile: PsiFile

  fun isInlayElement(psi: PsiElement): Boolean

  fun getInlayOutputs(inlayElement: PsiElement): List<InlayOutput>

  fun onUpdateHighlighting(toolbarElements: Collection<PsiElement>)

  fun getToolbarActions(toolbarElement: PsiElement): ActionGroup?

  fun isToolbarActionElement(psi: PsiElement): Boolean

  fun cleanup(psi: PsiElement): Future<Void>
}

interface InlayDescriptorProvider {

  fun getInlayDescriptor(editor: Editor): InlayElementDescriptor?

  companion object {
    val EP = ExtensionPointName.create<InlayDescriptorProvider>("com.intellij.datavis.r.inlays.inlayDescriptorProvider")
  }
}