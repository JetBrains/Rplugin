package org.jetbrains.r.editor.ui

import com.intellij.notebooks.images.InlayOutputImg
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.ui.RelativeFont
import com.intellij.util.ui.StartupUiUtil
import com.intellij.util.ui.UIUtil
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.*
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.progress.InlayProgressStatus
import org.jetbrains.r.rendering.chunk.ChunkImageInlayOutput
import java.awt.BorderLayout
import java.awt.Font
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import kotlin.math.min

/** Notebook console logs, HTML, and table result view. */
class NotebookInlayOutput(private val editor: Editor, private val parent: Disposable) : NotebookInlayState(), ToolBarProvider {

  init {
    layout = BorderLayout()
  }

  companion object {
    private const val RESIZE_TASK_NAME = "Resize graphics"
    private const val RESIZE_TASK_IDENTITY = "Resizing graphics"
    private const val RESIZE_TIME_SPAN = 500

    private val monospacedFont = RelativeFont.NORMAL.family(Font.MONOSPACED)
    private val outputFont = monospacedFont.derive(StartupUiUtil.labelFont.deriveFont(UIUtil.getFontSize(UIUtil.FontSize.SMALL)))
  }

  private var output: InlayOutput? = null


  private fun addTableOutput() = createOutput { parent, editor -> InlayOutputTable(parent, editor) }

  private fun addTextOutput() = createOutput {  parent, editor ->  InlayOutputText(parent, editor) }

  private fun addHtmlOutput() = createOutput {  parent, editor ->  InlayOutputHtml(parent, editor) }

  private fun addImgOutput() = createOutput {  parent, editor ->  InlayOutputImg(parent, editor) }

  private inline fun <T: InlayOutput> createOutput(constructor: (Disposable, Editor) -> T) =
    constructor(parent, editor).apply { setupOutput(this) }

  private fun setupOutput(output: InlayOutput) {
    this.output?.let { remove(it.getComponent()) }
    this.output = output
    output.onHeightCalculated = { height -> onHeightCalculated?.invoke(height) }
    add(output.getComponent(), BorderLayout.CENTER)

    addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        if (output.isFullWidth) {
          output.getComponent().bounds = Rectangle(0, 0, e.component.bounds.width, e.component.bounds.height)
        } else {
          output.getComponent().bounds = Rectangle(0, 0, min(output.getComponent().preferredSize.width, e.component.bounds.width),
                                                   e.component.bounds.height)
        }
      }
    })
    if (addToolbar) {
      output.addToolbar()
    }
  }

  private var addToolbar = false

  fun addToolbar() {
    addToolbar = true
    output?.addToolbar()
  }

  private fun getOrAddTextOutput(): InlayOutputText {
    (output as? InlayOutputText)?.let { return it }
    return addTextOutput()
  }

  fun addData(type: String, data: String, progressStatus: InlayProgressStatus?) {
    val inlayOutput: InlayOutput = when (type) {
      "IMG" -> createOutput { parent, editor -> ChunkImageInlayOutput(parent, editor) }
      "TABLE" -> output?.takeIf { it is InlayOutputTable } ?: addTableOutput()
      "HTML", "URL" -> output?.takeIf { it is InlayOutputHtml } ?: addHtmlOutput()
      "IMGBase64", "IMGSVG" -> output?.takeIf { it is InlayOutputImg } ?: addImgOutput()
      else -> getOrAddTextOutput()
    }
    progressStatus?.let {
      inlayOutput.updateProgressStatus(editor, it)
    }
    inlayOutput.addData(data, type)
  }

  fun addText(message: String, outputType: Key<*>) {
    getOrAddTextOutput().addData(message, outputType)
  }

  override fun updateProgressStatus(progressStatus: InlayProgressStatus) {
    output?.updateProgressStatus(editor, progressStatus)
  }

  override fun clear() {
    output?.clear()
  }

  override fun getCollapsedDescription(): String {
    return if (output == null) "" else output!!.getCollapsedDescription()
  }

  override fun onViewportChange(isInViewport: Boolean) {
    output?.onViewportChange(isInViewport)
  }

  override fun createActions(): List<AnAction> = output?.actions ?: emptyList()
}
