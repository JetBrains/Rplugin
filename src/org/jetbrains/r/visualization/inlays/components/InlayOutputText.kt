package org.jetbrains.r.visualization.inlays.components

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.SoftWrapChangeListener
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.softwrap.EmptySoftWrapPainter
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.RPluginCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.jetbrains.r.visualization.inlays.InlayExecutor
import org.jetbrains.r.visualization.inlays.InlayOutputData
import org.jetbrains.r.visualization.inlays.MouseWheelUtils
import org.jetbrains.r.visualization.ui.updateOutputTextConsoleUI
import java.awt.Dimension
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.readText
import kotlin.math.max
import kotlin.math.min

class InlayOutputText(parent: Disposable, editor: Editor)
  : InlayOutput(editor, loadActions(SaveOutputAction.Companion.ID)), InlayOutput.WithSaveAs {

  private val console = ColoredTextConsole(project, viewer = true)

  private val scrollPaneTopBorderHeight = 5

  init {
    Disposer.register(parent, console)
    toolbarPane.dataComponent = console.component

    val consoleEditor = console.editor as EditorEx
    MouseWheelUtils.initOutputTextConsole(editor, parent, consoleEditor, scrollPaneTopBorderHeight)
    ApplicationManager.getApplication().messageBus.connect(console)
      .subscribe(EditorColorsManager.TOPIC, EditorColorsListener {
        updateOutputTextConsoleUI(consoleEditor, editor)
        consoleEditor.component.repaint()
      })
  }

  override fun clear() {
    console.clear()
  }

  fun addData(data: InlayOutputData.TextOutput) {
    RPluginCoroutineScope.getScope(project).launch(ModalityState.defaultModalityState().asContextElement() + Dispatchers.IO) {
      val outputs = InlayExecutor.mutex.withLock {
        data.path.takeIf { it.exists() && it.extension == "json" }?.let { file ->
          Gson().fromJson<List<ProcessOutput>>(file.readText(), object : TypeToken<List<ProcessOutput>>() {}.type)
        }
      }

      edtWriteAction {
        if (outputs == null) {
          // DS-763 "\r\n" patterns would trim the whole last line.
          // todo looks like this an error, because text data contains path instead of text
          console.addData(data.path.toString().replace("\r\n", "\n").trimEnd('\n'), ProcessOutputType.STDOUT)
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
    val title = RBundle.message("inlay.action.export.as.txt.title")
    val description = RBundle.message("inlay.action.export.as.txt.description")
    val label = RBundle.message("inlay.action.export.as.txt.label")
    saveWithFileChooser(title, description, label, arrayOf("txt")) { destination ->
      destination.bufferedWriter().use { out ->
        out.write(console.text)
      }
    }
  }
}
