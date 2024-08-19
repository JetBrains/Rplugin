package org.jetbrains.r.editor.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.progress.InlayProgressStatus
import org.jetbrains.r.rendering.chunk.ChunkImageInlayOutput
import org.jetbrains.r.visualization.inlays.components.*
import java.awt.BorderLayout
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import kotlin.math.min

/** Notebook console logs, HTML, and table result view. */
class NotebookInlayOutput(private val editor: Editor, private val parent: Disposable) : NotebookInlayState(), ToolBarProvider {

  init {
    layout = BorderLayout()
  }

  private var output: InlayOutput? = null

  private inline fun <reified T: InlayOutput> createOutputAndSetup(constructor: (Disposable, Editor) -> T) =
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
    return createOutputAndSetup(::InlayOutputText)
  }

  fun addData(type: String, data: String, progressStatus: InlayProgressStatus?) {
    val inlayOutput: InlayOutput = when (type) {
      "IMG" -> createOutputAndSetup(::ChunkImageInlayOutput)
      "TABLE" -> output?.takeIf { it is InlayOutputTable } ?: createOutputAndSetup(::InlayOutputTable)
      "HTML", "URL" -> output?.takeIf { it is InlayOutputHtml } ?: createOutputAndSetup(::InlayOutputHtml)
      "IMGBase64", "IMGSVG" -> output?.takeIf { it is InlayOutputImg } ?: createOutputAndSetup(::InlayOutputImg)
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
