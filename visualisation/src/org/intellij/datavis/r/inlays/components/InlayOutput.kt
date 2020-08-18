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
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.SoftWrapChangeListener
import com.intellij.openapi.editor.impl.EditorImpl
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
import org.intellij.datavis.r.inlays.ClipboardUtils
import org.intellij.datavis.r.inlays.MouseWheelUtils
import org.intellij.datavis.r.inlays.runAsyncInlay
import org.intellij.datavis.r.ui.ToolbarUtil
import java.awt.Dimension
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
  //  when accessing `toolbarPane` inside constructor and when `toolbarPane` accesses `this`. So, be careful.
  //  Since this is an abstract class with many inheritors, the only way to get rid of this issue is to convert
  //  the class to the interface (or make the constructor private) and initialize `toolbarPane` inside some
  //  factory method.
  @Suppress("LeakingThis")
  protected val toolbarPane = ToolbarPane(this)

  protected val project: Project = editor.project ?: error("Editor should have a project")

  protected open val useDefaultSaveAction: Boolean = true
  protected open val extraActions: List<AnAction> = emptyList()

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
    return ToolbarUtil.createAnActionButton("org.intellij.datavis.r.inlays.components.ClearOutputAction", clearAction::invoke)
  }

  private fun createSaveAsAction(): AnAction {
    return ToolbarUtil.createAnActionButton("org.intellij.datavis.r.inlays.components.SaveOutputAction", this::saveAs)
  }
}

class InlayOutputImg(
  private val parent: Disposable,
  editor: Editor,
  clearAction: () -> Unit)
  : InlayOutput(parent, editor, clearAction) {
  private val wrapper = GraphicsPanelWrapper(project, parent).apply {
    isVisible = false
  }

  private val path2Checks = mutableMapOf<String, Boolean>()

  private val graphicsManager: GraphicsManager?
    get() = GraphicsManager.getInstance(project)

  private var globalResolutionSubscription: Disposable? = null

  @Volatile
  private var globalResolution: Int? = null

  override val useDefaultSaveAction = false
  override val extraActions = createExtraActions()

  init {
    toolbarPane.dataComponent = wrapper.component
    graphicsManager?.let { manager ->
      globalResolution = manager.globalResolution
      val connection = manager.addGlobalResolutionListener { newGlobalResolution ->
        wrapper.targetResolution = newGlobalResolution
        globalResolution = newGlobalResolution
      }
      Disposer.register(parent, connection)
    }
  }

  override fun addToolbar() {
    super.addToolbar()
    wrapper.overlayComponent = toolbarPane.toolbarComponent
  }

  override fun addData(data: String, type: String) {
    if (type == "IMG") {
      wrapper.isAutoResizeEnabled = false
      wrapper.addImage(File(data), GraphicsPanelWrapper.RescaleMode.LEFT_AS_IS, ::runAsyncInlay)
    } else {
      runAsyncInlay {
        when (type) {
          "IMGBase64" -> wrapper.showImageBase64(data)
          "IMGSVG" -> wrapper.showSvgImage(data)
          else -> Unit
        }
      }
    }.onSuccess {
      SwingUtilities.invokeLater {
        val maxHeight = wrapper.maximumHeight ?: 0
        val scaleMultiplier = if (UIUtil.isRetina()) 2 else 1
        val maxWidth = wrapper.maximumWidth ?: 0
        val editorWidth = editor.contentComponent.width
        if (maxWidth * scaleMultiplier <= editorWidth) {
          onHeightCalculated?.invoke(maxHeight * scaleMultiplier)
        } else {
          onHeightCalculated?.invoke(maxHeight * editorWidth / maxWidth)
        }
        if (type == "IMG") {
          wrapper.isAutoResizeEnabled = graphicsManager?.canRescale(data) ?: false
        }
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
    val imagePath = wrapper.imagePath
    if (imagePath != null && graphicsManager?.canRescale(imagePath) == true) {
      GraphicsExportDialog(project, parent, imagePath, wrapper.preferredImageSize).show()
    } else {
      wrapper.image?.let { image ->
        InlayOutputUtil.saveImageWithFileChooser(project, image)
      }
    }
  }

  override fun acceptType(type: String): Boolean {
    return type == "IMG" || type == "IMGBase64" || type == "IMGSVG"
  }

  override fun onViewportChange(isInViewport: Boolean) {
    wrapper.isVisible = isInViewport
  }

  private fun createExtraActions(): List<AnAction> {
    return listOf(
      ToolbarUtil.createAnActionButton("org.intellij.datavis.r.inlays.components.ExportImageAction", this::saveAs),
      ToolbarUtil.createAnActionButton("org.intellij.datavis.r.inlays.components.CopyImageToClipboardAction", this::copyImageToClipboard),
      ToolbarUtil.createAnActionButton("org.intellij.datavis.r.inlays.components.ZoomImageAction", this::canZoomImage, this::zoomImage),
      ToolbarUtil.createAnActionButton("org.intellij.datavis.r.inlays.components.ImageSettingsAction", this::openImageSettings)
    )
  }

  private fun copyImageToClipboard() {
    wrapper.image?.let { image ->
      ClipboardUtils.copyImageToClipboard(image)
    }
  }

  private fun zoomImage() {
    wrapper.imagePath?.let { path ->
      GraphicsZoomDialog(project, parent, path).show()
    }
  }

  private fun canZoomImage(): Boolean {
    return canZoomImageOrNull() == true
  }

  private fun canZoomImageOrNull(): Boolean? {
    return wrapper.imagePath?.let { path ->
      path2Checks.getOrPut(path) {  // Note: speedup FS operations caused by `canRescale(path)`
        graphicsManager?.canRescale(path) ?: false
      }
    }
  }

  private fun openImageSettings() {
    val isDarkEditor = EditorColorsManager.getInstance().isDarkEditor
    val isDarkModeEnabled = if (isDarkEditor) graphicsManager?.isDarkModeEnabled else null
    val initialSettings = getInitialSettings(isDarkModeEnabled)
    val dialog = GraphicsSettingsDialog(initialSettings) { newSettings ->
      wrapper.isAutoResizeEnabled = newSettings.isAutoResizedEnabled
      wrapper.targetResolution = newSettings.localResolution
      graphicsManager?.let { manager ->
        if (newSettings.isDarkModeEnabled != null && newSettings.isDarkModeEnabled != isDarkModeEnabled) {
          manager.isDarkModeEnabled = newSettings.isDarkModeEnabled
        }
        if (newSettings.globalResolution != null && newSettings.globalResolution != globalResolution) {
          // Note: no need to set `this.globalResolution` here: it will be changed automatically by a listener below
          manager.globalResolution = newSettings.globalResolution
        }
      }
    }
    dialog.show()
  }

  private fun getInitialSettings(isDarkModeEnabled: Boolean?) = GraphicsSettingsDialog.Settings(
    wrapper.isAutoResizeEnabled,
    isDarkModeEnabled,
    globalResolution,
    wrapper.localResolution
  )
}

class InlayOutputText(parent: Disposable, editor: Editor, clearAction: () -> Unit) : InlayOutput(parent, editor, clearAction) {

  private val console = ColoredTextConsole(project, viewer = true)

  private val maxHeight = 500

  init {
    Disposer.register(parent, console)
    toolbarPane.dataComponent = console.component

    (console.editor as EditorImpl).apply {
      backgroundColor = UIUtil.getPanelBackground()
      scrollPane.border = IdeBorderFactory.createEmptyBorder(JBUI.insets(5, 0, 0, 0))
      MouseWheelUtils.wrapMouseWheelListeners(scrollPane, parent)
    }

    console.editor.contentComponent.putClientProperty("AuxEditorComponent", true)

    val actionNameSelect = "TEXT_OUTPUT_SELECT_ALL"
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

          with(console.editor as EditorImpl) {
            softWrapModel.addSoftWrapChangeListener(
              object : SoftWrapChangeListener {
                override fun recalculationEnds() {
                  val height = offsetToXY(document.textLength).y + lineHeight + 5
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
