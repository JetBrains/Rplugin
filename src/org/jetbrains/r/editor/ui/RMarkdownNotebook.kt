package org.jetbrains.r.editor.ui

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import com.intellij.notebooks.visualization.NotebookCellLines
import com.intellij.notebooks.visualization.NotebookIntervalPointer
import com.intellij.notebooks.visualization.NotebookIntervalPointerFactory
import org.jetbrains.r.editor.ui.RMarkdownOutputInlayControllerUtil.getCodeFenceEnd


// todo notebook should be related to document or virtual file because there could be several editors
class RMarkdownNotebook(project: Project, editor: EditorImpl) {
  // pointerFactory reuses pointers
  private val outputs: MutableMap<NotebookIntervalPointer, RMarkdownNotebookOutput> = LinkedHashMap()
  private val pointerFactory = NotebookIntervalPointerFactory.Companion.get(editor)
  private val psiToInterval = PsiToInterval(project, editor) { interval -> getCodeFenceEnd(editor, interval) }

  operator fun get(cell: NotebookCellLines.Interval?): RMarkdownNotebookOutput? {
    if (cell == null) return null
    val intervalPointer = ReadAction.compute<NotebookIntervalPointer, Throwable> {
      pointerFactory.create(cell)
    }
    return outputs[intervalPointer]
  }

  operator fun get(cell: PsiElement): RMarkdownNotebookOutput? =
    correctCell(cell)?.let { psiToInterval[it] }?.let { this[it] }

  private fun correctCell(cell: PsiElement): PsiElement? =
    when (cell.elementType) {
      MarkdownTokenTypes.FENCE_LANG -> cell.parent.children.find { it.elementType == MarkdownTokenTypes.CODE_FENCE_END }
      else -> cell
    }

  fun update(output: RMarkdownNotebookOutput) {
    require(output.intervalPointer !in outputs)
    outputs[output.intervalPointer] = output
  }

  fun remove(output: RMarkdownNotebookOutput) {
    outputs.remove(output.intervalPointer, output)
  }

  companion object {
    private fun install(editor: EditorImpl): RMarkdownNotebook =
      RMarkdownNotebook(editor.project!!, editor).also {
        key.set(editor, it)
      }

    fun installIfNotExists(editor: EditorImpl): RMarkdownNotebook =
      editor.rMarkdownNotebook ?: install(editor)
  }
}

private val key = Key.create<RMarkdownNotebook>(RMarkdownNotebook::class.java.name)

val Editor.rMarkdownNotebook: RMarkdownNotebook?
  get() = key.get(this)
