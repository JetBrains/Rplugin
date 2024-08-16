package org.jetbrains.r.visualization.inlays.components

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.SoftWrapChangeListener
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.notebooks.visualization.r.VisualizationBundle
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.EmptySoftWrapPainter
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.InlayOutput
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.ProcessOutput
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.SaveOutputAction
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.initOutputTextConsole
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.updateOutputTextConsoleUI
import org.jetbrains.plugins.notebooks.visualization.r.inlays.runAsyncInlay
import java.awt.Dimension
import java.io.File
import javax.swing.SwingUtilities
import kotlin.math.max
import kotlin.math.min

class InlayOutputText(parent: Disposable, editor: Editor)
  : InlayOutput(parent, editor, loadActions(SaveOutputAction.Companion.ID)), InlayOutput.WithSaveAs {

  private val console = ColoredTextConsole(project, viewer = true)

  private val scrollPaneTopBorderHeight = 5

  init {
    Disposer.register(parent, console)
    toolbarPane.dataComponent = console.component

    val consoleEditor = console.editor as EditorEx
    initOutputTextConsole(editor, consoleEditor, scrollPaneTopBorderHeight)
    ApplicationManager.getApplication().messageBus.connect(console)
      .subscribe(EditorColorsManager.TOPIC, EditorColorsListener {
        updateOutputTextConsoleUI(consoleEditor, editor)
        consoleEditor.component.repaint()
      })
  }

  override fun clear() {
    console.clear()
  }

  override fun addData(data: String, type: String) {
    runAsyncInlay {
      File(data).takeIf { it.exists() && it.extension == "json" }?.let { file ->
        Gson().fromJson<List<ProcessOutput>>(file.readText(), object : TypeToken<List<ProcessOutput>>() {}.type)
      }.let { outputs ->
        SwingUtilities.invokeLater {
          if (outputs == null) {
            // DS-763 "\r\n" patterns would trim the whole last line.
            console.addData(data.replace("\r\n", "\n").trimEnd('\n'), ProcessOutputType.STDOUT)
          }
          else {
            outputs.forEach { console.addData(it.text, it.kind) }
          }
          console.flushDeferredText()

          (console.editor as? EditorImpl)?.apply {
            updateSize(this)

            softWrapModel.setSoftWrapPainter(EmptySoftWrapPainter)
            softWrapModel.addSoftWrapChangeListener(
              object : SoftWrapChangeListener {
                override fun recalculationEnds() = updateSize(this@apply)

                override fun softWrapsChanged() {}
              }
            )
          }
        }
      }
    }
  }

  private fun updateSize(editor: EditorImpl) {
    with(editor) {
      val textHeight = offsetToXY(document.textLength).y + lineHeight + scrollPaneTopBorderHeight
      component.preferredSize = Dimension(preferredSize.width, textHeight)
      onHeightCalculated?.invoke(max(textHeight, toolbarPane.preferredSize.height))
    }
  }

  fun addData(message: String, outputType: Key<*>) {
    console.addData(message, outputType)
  }

  override fun scrollToTop() {
    console.scrollTo(0)
  }

  override fun getCollapsedDescription(): String {
    return console.text.substring(0, min(console.text.length, 60)) + " ....."
  }

  override fun acceptType(type: String): Boolean {
    return type == "TEXT"
  }

  override fun saveAs() {
    val title = VisualizationBundle.message("inlay.action.export.as.txt.title")
    val description = VisualizationBundle.message("inlay.action.export.as.txt.description")
    saveWithFileChooser(title, description, arrayOf("txt"), "output") { destination ->
      destination.bufferedWriter().use { out ->
        out.write(console.text)
      }
    }
  }
}