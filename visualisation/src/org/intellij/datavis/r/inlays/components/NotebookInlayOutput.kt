/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.ui.RelativeFont
import com.intellij.util.ui.UIUtil
import java.awt.Font
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.Icon

class ProcessOutput(val text: String, kind: Key<*>) {
  private val kindValue: Int = when(kind) {
    ProcessOutputTypes.STDOUT -> 1
    ProcessOutputTypes.STDERR -> 2
    else -> 3
  }

  val kind: Key<*>
    get() = when (kindValue) {
      1 -> ProcessOutputType.STDOUT
      2 -> ProcessOutputType.STDERR
      else -> ProcessOutputType.SYSTEM
    }
}


/** Notebook console logs and html result view. */
class NotebookInlayOutput(private val editor: Editor, private val parent: Disposable) : NotebookInlayState(), ToolBarProvider {

  companion object {
    private const val RESIZE_TASK_NAME = "Resize graphics"
    private const val RESIZE_TASK_IDENTITY = "Resizing graphics"
    private const val RESIZE_TIME_SPAN = 500

    private val monospacedFont = RelativeFont.NORMAL.family(Font.MONOSPACED)
    private val outputFont = monospacedFont.derive(UIUtil.getLabelFont().deriveFont(UIUtil.getFontSize(UIUtil.FontSize.SMALL)))
  }

  interface ActionHolder {
    val icon: Icon
    val text: String
    val description: String
    fun onClick()
  }

  private var output: InlayOutput? = null

  private fun addTextOutput() = createOutput {  parent, editor, clearAction ->  InlayOutputText(parent, editor.project!!, clearAction) }

  private fun addHtmlOutput() = createOutput {  parent, editor, clearAction ->  InlayOutputHtml(parent, editor.project!!, clearAction) }

  private fun addImgOutput() = createOutput {  parent, editor, clearAction ->  InlayOutputImg(parent, editor, clearAction) }

  private inline fun createOutput(constructor: (Disposable, Editor, () -> Unit) -> InlayOutput) =
    constructor(parent, editor, clearAction).apply { setupOutput(this) }

  private fun setupOutput(output: InlayOutput) {
    this.output?.let { remove(it.getComponent()) }
    this.output = output
    output.onHeightCalculated = { height -> onHeightCalculated?.invoke(height) }
    add(output.getComponent(), DEFAULT_LAYER)

    addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        output.getComponent().bounds = Rectangle(0, 0, e.component.bounds.width, e.component.bounds.height)
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

  fun addData(type: String, data: String) {
    val provider = InlayOutputProvider.EP.extensionList.asSequence().filter { it.acceptType(type) }.firstOrNull()
    if (provider != null) {
      (output.takeIf { it?.acceptType(type) == true } ?: createOutput { parent, editor, clearAction ->
        provider.create(parent, editor, clearAction)
      }).addData(data, type)
    } else {
      when (type) {
        "HTML", "URL" -> output?.takeIf { it is InlayOutputHtml } ?: addHtmlOutput()
        "IMG", "IMGBase64", "IMGSVG" -> output?.takeIf { it is InlayOutputImg } ?: addImgOutput()
        else -> output?.takeIf { it is InlayOutputText } ?: addTextOutput()
      }.addData(data, type)
    }
  }

  override fun  clear() {
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