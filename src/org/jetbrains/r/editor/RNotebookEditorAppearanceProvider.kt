package org.jetbrains.r.editor

import com.intellij.notebooks.ui.editor.DefaultNotebookEditorAppearance
import com.intellij.notebooks.ui.visualization.NotebookEditorAppearance
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.util.Key
import org.jetbrains.r.rmarkdown.RMarkdownVirtualFile
import java.awt.Color

object RNotebookEditorAppearanceProvider {
  fun create(editor: Editor): RMarkdownNotebookEditorAppearance? {
    if (RMarkdownVirtualFile.hasVirtualFile(editor)) {
      return RMarkdownNotebookEditorAppearance(editor)
    }
    return null
  }

  internal val R_NOTEBOOK_APPEARANCE_KEY: Key<RMarkdownNotebookEditorAppearance> =
    Key.create(RMarkdownNotebookEditorAppearance::class.java.name)

  fun install(editor: Editor) {
    editor.putUserData(R_NOTEBOOK_APPEARANCE_KEY, create(editor))
  }
}

val Editor.rNotebookAppearance: NotebookEditorAppearance
  get() = getUserData(RNotebookEditorAppearanceProvider.R_NOTEBOOK_APPEARANCE_KEY)!!


// todo remove editor from arguments at all, pass it as parameter where necessary
// todo convert RMarkdownNotebookEditorAppearance to singleton object, no need to install it at all and make provider for it
class RMarkdownNotebookEditorAppearance(editor: Editor) : DefaultNotebookEditorAppearance(editor) {

  override fun getInlayBackgroundColor(scheme: EditorColorsScheme): Color? = codeCellBackgroundColor.get()
  override fun shouldShowCellLineNumbers(): Boolean = false
  override fun shouldShowExecutionCounts(): Boolean = true

  override fun shouldShowOutExecutionCounts(): Boolean = true
  override fun shouldShowRunButtonInGutter(): Boolean = false

  // todo rm this method, it is unused in RMarkdown
  override fun getCellLeftLineWidth(editor: Editor): Int = 0
}