package org.jetbrains.plugins.notebooks.jupyter.editor.outputs

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.Disposer
import org.intellij.datavis.r.inlays.components.initOutputTextConsole
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * A wrapper for [ConsoleViewImpl] for showing stdout, stderr, textual output.
 */
class TextOutputComponent(editor: EditorImpl) : JPanel(BorderLayout()), Disposable {
  val console = ConsoleViewImpl(editor.project!!, true)
  private val scrollPaneTopBorderHeight = 5

  init {
    add(console.component, BorderLayout.NORTH)

    initOutputTextConsole(
      editor,
      this,
      console,
      scrollPaneTopBorderHeight,
      editor.colorsScheme.defaultBackground,
    )
  }

  override fun dispose() {
    Disposer.dispose(console)
  }
}