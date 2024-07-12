package org.jetbrains.r.quarto

import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.fileTypes.EditorHighlighterProvider
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.r.rmarkdown.RMarkdownOrQuartoTemplateEditorHighlighter

class QuartoEditorHighlighterProvider : EditorHighlighterProvider {
  override fun getEditorHighlighter(project: Project?,
                                    fileType: FileType,
                                    virtualFile: VirtualFile?,
                                    colors: EditorColorsScheme): EditorHighlighter {
    return RMarkdownOrQuartoTemplateEditorHighlighter(project, virtualFile, colors)
  }
}