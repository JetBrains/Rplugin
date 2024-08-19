/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.visualization.inlays

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.r.visualization.inlays.components.InlayProgressStatus
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
   * returns true if the [psi] could have inlay outputs
   */
  fun isInlayElement(psi: PsiElement): Boolean

  /**
   * @return the output for the [inlayElement], empty list if there is no outputs for the [inlayElement] and null if descriptor doesn't
   * store inlay outputs
   * @param inlayElement `isInlayElement(inlayElement)` is true
   */
  fun getInlayOutputs(inlayElement: PsiElement): List<InlayOutput>? = null

  /**
   * the method is called when inlay output of [psi] is intentionally removed by a user.
   */
  fun cleanup(psi: PsiElement): Future<Void>? = null
}
