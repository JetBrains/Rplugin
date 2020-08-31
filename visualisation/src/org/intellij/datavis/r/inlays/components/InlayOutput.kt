/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.execution.process.ProcessOutputType
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.SoftWrapChangeListener
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.softwrap.SoftWrapDrawingType
import com.intellij.openapi.editor.impl.softwrap.SoftWrapPainter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.cef.browser.CefBrowser
import org.cef.handler.CefLoadHandlerAdapter
import org.intellij.datavis.r.VisualizationBundle
import org.intellij.datavis.r.inlays.ClipboardUtils
import org.intellij.datavis.r.inlays.InlayDimensions
import org.intellij.datavis.r.inlays.MouseWheelUtils
import org.intellij.datavis.r.inlays.runAsyncInlay
import org.intellij.datavis.r.ui.ToolbarUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.concurrency.Promise
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.io.File
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import kotlin.math.min

abstract class InlayOutput(parent: Disposable, val editor: Editor, private val clearAction: () -> Unit) {
  // Transferring `this` from the constructor to another class violates JMM and leads to undefined behaviour
  // when accessing `toolbarPane` inside constructor and when `toolbarPane` accesses `this`. So, be careful.
  // Since this is an abstract class with many inheritors, the only way to get rid of this issue is to convert
  // the class to the interface (or make the constructor private) and initialize `toolbarPane` inside some
  // factory method.
  @Suppress("LeakingThis")
  protected val toolbarPane = ToolbarPane(this)

  protected val project: Project = editor.project ?: error("Editor should have a project")

  protected open val useDefaultSaveAction: Boolean = true
  protected open val extraActions: List<AnAction> = emptyList()

  /** If the output should occupy as much editor width as possible. */
  open val isFullWidth = true

  val actions: List<AnAction> by lazy {  // Note: eager initialization will cause runtime errors
    createActions()
  }

  fun getComponent() = toolbarPane

  /** Clears view, removes text/html. */
  abstract fun clear()

  abstract fun addData(data: String, type: String)
  abstract fun scrollToTop()
  abstract fun getCollapsedDescription(): String

  abstract fun saveAs()

  abstract fun acceptType(type: String): Boolean

  fun updateProgressStatus(progressStatus: InlayProgressStatus) {
    toolbarPane.progressComponent = buildProgressStatusComponent(progressStatus)
  }

  private fun getProgressStatusHeight(): Int {
    return toolbarPane.progressComponent?.height ?: 0
  }

  /**
   * HTML output returns the height delayed from it's Platform.runLater.
   */
  var onHeightCalculated: ((height: Int) -> Unit)? = null
    set(value) {
      field = { height: Int ->
        value?.invoke(height + getProgressStatusHeight())
      }
    }

  private val disposable: Disposable = Disposer.newDisposable()

  init {
    Disposer.register(parent, disposable)
  }

  open fun onViewportChange(isInViewport: Boolean) {
    // Do nothing by default
  }

  open fun addToolbar() {
    toolbarPane.toolbarComponent = createToolbar()
  }

  private fun createToolbar(): JComponent {
    val ellipsis = DefaultActionGroup().apply {
      addAll(actions)
      add(createClearAction())
      isPopup = true
      with(templatePresentation) {
        putClientProperty(ActionButton.HIDE_DROPDOWN_ICON, true)
        icon = AllIcons.Actions.More
      }
    }
    val toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, DefaultActionGroup(ellipsis), false)
    return toolbar.component.apply {
      isOpaque = true
      background = UIUtil.getEditorPaneBackground()
    }
  }

  protected fun saveWithFileChooser(title: String, description: String, extension: Array<String>, defaultName: String, onChoose: (File) -> Unit) {
    InlayOutputUtil.saveWithFileChooser(project, title, description, extension, defaultName, true, onChoose)
  }

  private fun createActions(): List<AnAction> {
    return extraActions.toMutableList().apply {
      if (useDefaultSaveAction) {
        add(createSaveAsAction())
      }
    }
  }

  private fun createClearAction(): AnAction {
    return ToolbarUtil.createAnActionButton<ClearOutputAction>(clearAction::invoke)
  }

  private fun createSaveAsAction(): AnAction {
    return ToolbarUtil.createAnActionButton<SaveOutputAction>(this::saveAs)
  }
}

class InlayOutputImg(parent: Disposable, editor: Editor, clearAction: () -> Unit) : InlayOutput(parent, editor, clearAction) {
  private val graphicsPanel = GraphicsPanel(project, parent).apply {
    isAdvancedMode = true
  }

  override val extraActions = createExtraActions()
  override val isFullWidth = false

  init {
    toolbarPane.dataComponent = graphicsPanel.component
  }

  override fun addToolbar() {
    super.addToolbar()
    graphicsPanel.overlayComponent = toolbarPane.toolbarComponent
  }

  override fun addData(data: String, type: String) {
    showImageAsync(data, type).onSuccess {
      SwingUtilities.invokeLater {
        val maxHeight = graphicsPanel.maximumSize?.height ?: 0
        val maxWidth = graphicsPanel.maximumSize?.width ?: 0
        val height = InlayDimensions.calculateInlayHeight(maxWidth, maxHeight, editor)
        onHeightCalculated?.invoke(height)
      }
    }
  }

  private fun showImageAsync(data: String, type: String): Promise<Unit> {
    return runAsyncInlay {
      when (type) {
        "IMGBase64" -> graphicsPanel.showImageBase64(data)
        "IMGSVG" -> graphicsPanel.showSvgImage(data)
        "IMG" -> graphicsPanel.showImage(File(data))
        else -> Unit
      }
    }
  }

  override fun clear() {
  }

  override fun scrollToTop() {
  }

  override fun getCollapsedDescription(): String {
    return "foo"
  }

  override fun saveAs() {
    graphicsPanel.image?.let { image ->
      InlayOutputUtil.saveImageWithFileChooser(project, image)
    }
  }

  override fun acceptType(type: String): Boolean {
    return type == "IMG" || type == "IMGBase64" || type == "IMGSVG"
  }

  private fun createExtraActions(): List<AnAction> {
    return listOf(ToolbarUtil.createAnActionButton<CopyImageToClipboardAction>(this::copyImageToClipboard))
  }

  private fun copyImageToClipboard() {
    graphicsPanel.image?.let { image ->
      ClipboardUtils.copyImageToClipboard(image)
    }
  }
}

class InlayOutputText(parent: Disposable, editor: Editor, clearAction: () -> Unit) : InlayOutput(parent, editor, clearAction) {

  private val console = ColoredTextConsole(project, viewer = true)

  private val maxHeight = 500
  private val scrollPaneTopBorderHeight = 5
  init {
    Disposer.register(parent, console)
    toolbarPane.dataComponent = console.component

    (console.editor as EditorImpl).apply {
      backgroundColor = UIUtil.getPanelBackground()
      scrollPane.border = IdeBorderFactory.createEmptyBorder(JBUI.insets(scrollPaneTopBorderHeight, 0, 0, 0))
      MouseWheelUtils.wrapMouseWheelListeners(scrollPane, parent)
    }

    console.editor.contentComponent.putClientProperty("AuxEditorComponent", true)

    @NonNls
    val actionNameSelect = VisualizationBundle.message("action.name.output.select.all")
    val actionSelect = object : AbstractAction(actionNameSelect) {
      override fun actionPerformed(e: ActionEvent) {
        (console.editor as EditorImpl).selectionModel.setSelection(0, console.text.length)
      }
    }
    console.editor.contentComponent.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_DOWN_MASK),
                                                 actionNameSelect)
    console.editor.contentComponent.actionMap.put(actionNameSelect, actionSelect)

    console.editor.settings.isUseSoftWraps = true
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
            console.addData(data, ProcessOutputType.STDOUT)
          }
          else {
            outputs.forEach { console.addData(it.text, it.kind) }
          }
          console.flushDeferredText()

          (console.editor as? EditorImpl)?.apply {
            softWrapModel.setSoftWrapPainter(object : SoftWrapPainter {
              override fun paint(g: Graphics, drawingType: SoftWrapDrawingType, x: Int, y: Int, lineHeight: Int) = 0

              override fun getDrawingHorizontalOffset(g: Graphics, drawingType: SoftWrapDrawingType, x: Int, y: Int, lineHeight: Int) = 0

              override fun getMinDrawingWidth(drawingType: SoftWrapDrawingType) = 0

              override fun canUse() = true

              override fun reinit() {}
            })
            softWrapModel.addSoftWrapChangeListener(
              object : SoftWrapChangeListener {
                override fun recalculationEnds() {
                  val height = offsetToXY(document.textLength).y + lineHeight + scrollPaneTopBorderHeight
                  component.preferredSize = Dimension(preferredSize.width, height)
                  onHeightCalculated?.invoke(min(height, maxHeight))
                }

                override fun softWrapsChanged() {}
              }
            )
          }
        }
      }
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
    return  type == "TEXT"
  }

  override fun saveAs() {
    val title = "Export as txt"
    val description = "Export console content to text file"
    saveWithFileChooser(title, description, arrayOf("txt"), "output") { destination ->
      destination.bufferedWriter().use { out ->
        out.write(console.text)
      }
    }
  }
}

class InlayOutputHtml(parent: Disposable, editor: Editor, clearAction: () -> Unit) : InlayOutput(parent, editor, clearAction) {

  private val jbBrowser: JBCefBrowser = JBCefBrowser().also { Disposer.register(parent, it) }
  private val heightJsCallback = JBCefJSQuery.create(jbBrowser)
  private val saveJsCallback = JBCefJSQuery.create(jbBrowser)
  private var height: Int = 0

  init {
    MouseWheelUtils.wrapMouseWheelListeners(jbBrowser.component, parent)
    heightJsCallback.addHandler {
      val height = it.toInt()
      if (this.height != height) {
        this.height = height
        invokeLater {
          SwingUtilities.invokeLater {
            onHeightCalculated?.invoke(height)
          }
        }
      }
      JBCefJSQuery.Response("OK")
    }
    Disposer.register(jbBrowser, heightJsCallback)
    toolbarPane.dataComponent = jbBrowser.component
  }

  override fun acceptType(type: String): Boolean {
    return  type == "HTML" || type == "URL"
  }

  override fun clear() {}

  private fun notifySize() {
    jbBrowser.cefBrowser.executeJavaScript(
      "var body = document.body,"
      + "html = document.documentElement;"
      + "var height = Math.max( body.scrollHeight, body.offsetHeight, html.clientHeight, html.scrollHeight , html.offsetHeight );"
      + "window.${heightJsCallback.funcName}({request: String(height)});",
      jbBrowser.cefBrowser.url, 0
    )
  }

  override fun addData(data: String, type: String) {
    val isUrl = data.startsWith("file://") || data.startsWith("http://") || data.startsWith("https://")
    if (isUrl) {
      jbBrowser.loadURL(data)
    }
    else {
      jbBrowser.loadHTML("<head><style>" + GithubMarkdownCss.css + " </style></head><body>" + data + "</body>")
    }
    jbBrowser.jbCefClient.addLoadHandler( object : CefLoadHandlerAdapter() {
      override fun onLoadingStateChange(browser: CefBrowser?, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
        notifySize()
      }
    }, jbBrowser.cefBrowser)
  }

  // For HTML component no need to scroll to top, because it is not scrolling to end.
  override fun scrollToTop() {}

  override fun getCollapsedDescription(): String {
    return "html output"
  }

  override fun saveAs() {
    val title = "Export as txt"
    val description = "Exports the selected range or whole table if nothing is selected as csv or tsv file"
    saveWithFileChooser(title, description, arrayOf("txt"), "output") { destination ->
      saveJsCallback.addHandler(object : java.util.function.Function<String, JBCefJSQuery.Response> {
        override fun apply(selection: String): JBCefJSQuery.Response {
          destination.bufferedWriter().use { out ->
            out.write(selection)
          }
          saveJsCallback.removeHandler(this)
          return JBCefJSQuery.Response("OK")
        }
      })
      jbBrowser.cefBrowser.executeJavaScript("window.${saveJsCallback.funcName}({request: window.getSelection().toString()})",
                                             jbBrowser.cefBrowser.url, 0)
    }
  }
}
