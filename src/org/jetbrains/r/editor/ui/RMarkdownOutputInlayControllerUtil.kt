package org.jetbrains.r.editor.ui

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.jetbrains.r.visualization.RNotebookCellLines
import org.jetbrains.r.visualization.RNotebookCellLines.Interval
import org.jetbrains.r.visualization.inlays.EditorInlaysManager
import org.jetbrains.r.visualization.inlays.InlayComponent
import org.jetbrains.r.visualization.inlays.RInlayDimensions
import java.awt.Point
import java.awt.Rectangle

internal object RMarkdownOutputInlayControllerUtil {
  internal fun addBlockElement(editor: Editor, offset: Int, inlayComponent: NotebookInlayComponent): Inlay<NotebookInlayComponent> =
    editor.inlayModel.addBlockElement(offset, true, false, EditorInlaysManager.INLAY_PRIORITY, inlayComponent)!!

  internal fun setupInlayComponent(editor: Editor, inlayComponent: NotebookInlayComponent) {
    val scrollKeeper = EditorScrollingPositionKeeper(editor)

    fun updateInlaysInEditor(editor: Editor) {
      val end = editor.xyToLogicalPosition(Point(0, Int.MAX_VALUE))
      val offsetEnd = editor.logicalPositionToOffset(end)

      val inlays = editor.inlayModel.getBlockElementsInRange(0, offsetEnd)

      inlays.forEach { inlay ->
        if (inlay.renderer is InlayComponent) {
          (inlay.renderer as InlayComponent).updateComponentBounds(inlay)
        }
      }
    }
    inlayComponent.beforeHeightChanged = {
      scrollKeeper.savePosition()
    }
    inlayComponent.afterHeightChanged = {
      updateInlaysInEditor(editor)
      scrollKeeper.restorePosition(true)
    }
  }

  internal fun getPsiElement(editor: Editor, offset: Int): PsiElement? =
    editor.psiFile?.viewProvider?.let { it.findElementAt(offset, it.baseLanguage) }

  internal fun getCodeFenceEnd(psiElement: PsiElement): PsiElement? =
    psiElement.let { it.parent.children.find { it.elementType == MarkdownTokenTypes.CODE_FENCE_END } }

  internal fun getCodeFenceEnd(editor: EditorImpl, interval: RNotebookCellLines.Interval): PsiElement? {
    val offset = extractOffset(editor.document, interval)
    val psiElement = getPsiElement(editor, offset) ?: return null
    return getCodeFenceEnd(psiElement)
  }

  internal fun disposeComponent(component: NotebookInlayComponent) {
    component.parent?.remove(component)
    component.disposeInlay()
    component.dispose()
  }

  internal fun extractOffset(document: Document, interval: Interval): Int =
    Integer.max(document.getLineEndOffset(interval.lines.last) - 1, 0)

  internal fun isInViewportByY(editor: EditorEx, rect: Rectangle): Boolean {
    val viewRect = editor.scrollPane.viewport.viewRect
    return rect.y <= viewRect.y + viewRect.height &&
           rect.y + rect.height >= viewRect.y
  }

  internal fun updateInlayWidth(editor: EditorEx, inlayComponent: NotebookInlayComponent) {
    val expectedWidth = RInlayDimensions.calculateInlayWidth(editor)
    if (expectedWidth > 0 && inlayComponent.width != expectedWidth) {
      inlayComponent.setSize(expectedWidth, inlayComponent.height)
      inlayComponent.inlay?.update()
    }
  }
}