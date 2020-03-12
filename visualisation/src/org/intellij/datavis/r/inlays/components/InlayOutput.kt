/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.execution.process.ProcessOutputType
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.CopyProvider
import com.intellij.ide.DataManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileChooser.ex.FileSaverDialogImpl
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.ui.TextTransferable
import com.intellij.util.ui.UIUtil
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import org.intellij.datavis.r.VisualizationBundle
import org.intellij.datavis.r.inlays.ClipboardUtils
import org.intellij.datavis.r.inlays.MouseWheelUtils
import org.intellij.datavis.r.inlays.runAsyncInlay
import org.w3c.dom.events.EventTarget
import org.w3c.dom.html.HTMLAnchorElement
import java.awt.Color
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.io.File
import java.nio.file.Paths
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import kotlin.math.min

abstract class InlayOutput(parent: Disposable, protected val project: Project, private val clearAction: () -> Unit) {

  protected val toolbarPane = ToolbarPane()

  protected open val extraActions: List<NotebookInlayOutput.ActionHolder> = emptyList()

  val actions: List<AnAction> by lazy {  // Note: eager initialization will cause runtime errors
    createActions()
  }

  fun getComponent(): Component {
    return toolbarPane
  }

  /** Clears view, removes text/html. */
  abstract fun clear()

  abstract fun addData(data: String, type: String)
  abstract fun scrollToTop()
  abstract fun getCollapsedDescription(): String

  abstract fun saveAs()

  abstract fun acceptType(type: String): Boolean

  fun updateProgressStatus(progressStatus: InlayProgressStatus) {
    val progressPanel = buildProgressStatusComponent(progressStatus)
    toolbarPane.progressComponent = progressPanel
  }

  protected fun getProgressStatusHeight(): Int {
    return toolbarPane.progressComponent?.height ?: 0
  }

  /**
   * Inlay component can can adjust itself to fit the Output.
   * We need callback because text output can return height immediately,
   * but Html output can return height only delayed, from it's Platform.runLater.
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

  protected fun createToolbar(): JComponent {
    val allActions = actions + listOf(createClearAction())
    val actionGroup = DefaultActionGroup(allActions)
    val toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, actionGroup, true)
    return toolbar.component.apply {
      isOpaque = false
      background = Color(0, 0, 0, 0)
    }
  }

  protected fun saveWithFileChooser(title: String, description: String, extension: Array<String>, defaultName: String, onChoose: (File) -> Unit) {
    val descriptor = FileSaverDescriptor(title, description, *extension)
    val chooser = FileSaverDialogImpl(descriptor, project)
    val virtualBaseDir = VfsUtil.findFile(Paths.get(project.basePath!!), true)
    chooser.save(virtualBaseDir, defaultName)?.let { fileWrapper ->
      val destination = fileWrapper.file
      try {
        createDestinationFile(destination)
        onChoose(destination)
      } catch (e: Exception) {
        val details = e.message?.let { ":\n$it" }
        notifyExportError("Cannot save to selected destination$details")
      }
    }
  }

  private fun createDestinationFile(file: File) {
    if (!file.exists() && !file.createNewFile()) {
      throw RuntimeException("Cannot create requested file")
    }
  }

  private fun notifyExportError(content: String) {
    val notification = Notification(VisualizationBundle.message("inlay.output.notification.group.name"),
                                    VisualizationBundle.message("inlay.output.export.failure"),
                                    content,
                                    NotificationType.ERROR)
    notification.notify(project)
  }

  private fun createActions(): List<AnAction> {
    val extraActions = createExtraActions() ?: emptyList()
    return extraActions + listOf(createSaveAsAction())
  }

  private fun createExtraActions(): List<AnAction>? {
    return extraActions.map { holder ->
      object : DumbAwareAction(holder.text, holder.description, holder.icon) {
        override fun actionPerformed(e: AnActionEvent) {
          holder.onClick()
        }
      }
    }
  }

  private fun createClearAction(): AnAction {
    return object : DumbAwareAction(VisualizationBundle.message("inlay.output.clear.text"),
                                    VisualizationBundle.message("inlay.output.clear.description"),
                                    AllIcons.Actions.GC) {
      override fun actionPerformed(e: AnActionEvent) {
        clearAction.invoke()
      }
    }
  }

  private fun createSaveAsAction(): AnAction {
    return object : DumbAwareAction(VisualizationBundle.message("inlay.output.save.as.text"),
                                    VisualizationBundle.message("inlay.output.save.as.description"),
                                    AllIcons.Actions.Menu_saveall) {
      override fun actionPerformed(e: AnActionEvent) {
        saveAs()
      }
    }
  }
}

class InlayOutputImg(
  parent: Disposable,
  private val editor: Editor,
  clearAction: () -> Unit)
  : InlayOutput(parent, editor.project!!, clearAction) {
  private val wrapper = GraphicsPanelWrapper(project, parent).apply {
    isVisible = false
  }

  private val copyActionHolder = object : NotebookInlayOutput.ActionHolder {
    override val icon = AllIcons.Actions.Copy
    override val text = "Copy plot to clipboard"
    override val description = "Copy this plot to the clipboard"

    override fun onClick() {
      wrapper.image?.let { image ->
        ClipboardUtils.copyImageToClipboard(image)
      }
    }
  }

  private val zoomActionHolder = object : NotebookInlayOutput.ActionHolder {
    override val icon = AllIcons.Actions.Preview
    override val text = VisualizationBundle.message("inlay.output.image.zoom.text")
    override val description = VisualizationBundle.message("inlay.output.image.zoom.description")

    override fun onClick() {
      wrapper.imagePath?.let { path ->
        GraphicsZoomDialog(project, parent, path).show()
      }
    }
  }

  private val settingsActionHolder = object : NotebookInlayOutput.ActionHolder {
    override val icon = AllIcons.General.GearPlain
    override val text = VisualizationBundle.message("inlay.output.image.settings.text")
    override val description = VisualizationBundle.message("inlay.output.image.settings.description")

    override fun onClick() {
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

  private val graphicsManager: GraphicsManager?
    get() = GraphicsManager.getInstance(project)

  private var globalResolutionSubscription: Disposable? = null

  @Volatile
  private var globalResolution: Int? = null

  override val extraActions: List<NotebookInlayOutput.ActionHolder> = listOf(copyActionHolder, zoomActionHolder, settingsActionHolder)

  init {
    toolbarPane.centralComponent = wrapper.component
    graphicsManager?.let { manager ->
      globalResolution = manager.globalResolution
      val connection = manager.addGlobalResolutionListener { newGlobalResolution ->
        wrapper.targetResolution = newGlobalResolution
        globalResolution = newGlobalResolution
      }
      Disposer.register(parent, connection)
    }
  }

  override fun addData(data: String, type: String) {
    if (type == "IMG") {
      wrapper.addImage(File(data), false, ::runAsyncInlay)
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
    wrapper.image?.let { image ->
      val title = VisualizationBundle.message("inlay.output.image.export.text")
      val description = VisualizationBundle.message("inlay.output.image.export.description")
      val imageTypeSpecifier = ImageTypeSpecifier.createFromRenderedImage(image)
      val extensions = arrayOf("png", "jpeg", "bmp", "gif", "tiff").filter {
        ImageIO.getImageWriters(imageTypeSpecifier, it).asSequence().any()
      }.toTypedArray()
      saveWithFileChooser(title, description, extensions, defaultName = "image") { destination ->
        ImageIO.write(image, destination.extension, destination)
      }
    }
  }

  override fun acceptType(type: String): Boolean {
    return type == "IMG" || type == "IMGBase64" || type == "IMGSVG"
  }

  override fun onViewportChange(isInViewport: Boolean) {
    wrapper.isVisible = isInViewport
  }
}

class InlayOutputText(parent: Disposable, project: Project, clearAction: () -> Unit) : InlayOutput(parent, project, clearAction) {

  private val console = ColoredTextConsole(project, viewer = true)

  init {
    Disposer.register(parent, console)
    toolbarPane.centralComponent = console.component
    (console.editor as EditorImpl).backgroundColor = UIUtil.getPanelBackground()
    (console.editor as EditorImpl).scrollPane.border = null
    MouseWheelUtils.wrapMouseWheelListeners((console.editor as EditorImpl).scrollPane, parent)

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
          } else {
            outputs.forEach { console.addData(it.text, it.kind) }
          }
          console.flushDeferredText()
          onHeightCalculated?.invoke(console.preferredSize.height)
        }
      }
    }
  }

  fun addData(message: String, outputType: Key<*>) = console.addData(message, outputType)

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

class WebViewCopyProvider(private val webView: WebView) : CopyProvider {

  override fun performCopy(dataContext: DataContext) {
    Platform.runLater {
      val selection: String? = webView.engine.executeScript("window.getSelection().toString()") as String
      CopyPasteManager.getInstance().setContents(TextTransferable(selection))
    }
  }

  override fun isCopyEnabled(dataContext: DataContext): Boolean {
    return true
  }

  override fun isCopyVisible(dataContext: DataContext): Boolean {
    return true
  }
}


// Running on jre 1.8 we can get an exception:
// Exception in thread "Thread-30" java.lang.NullPointerException
// at com.sun.webkit.MainThread.fwkScheduleDispatchFunctions(MainThread.java:34)
//
// https://intellij-support.jetbrains.com/hc/en-us/community/posts/360000895600-JavaFX-WebView-in-plugin-and-ClassLoader
// https://youtrack.jetbrains.com/issue/IDEA-199701
//
// when trying to load "<img src=\"data:image/png;base64,$img\">"
//
// The only way to display embedded images - use JRE 11.
class InlayOutputHtml(parent: Disposable, project: Project, clearAction: () -> Unit) : InlayOutput(parent, project, clearAction) {

  private val jfxPanel = JFXPanel()
  private lateinit var webView: WebView

  init {
    MouseWheelUtils.wrapMouseWheelListeners(jfxPanel, parent)
    toolbarPane.centralComponent = jfxPanel
    jfxPanel.putClientProperty("AuxEditorComponent", true)
  }

  override fun saveAs() {
    val title = "Export as txt"
    val description = "Exports the selected range or whole table if nothing is selected as csv or tsv file"
    saveWithFileChooser(title, description, arrayOf("txt"), "output") { destination ->
      Platform.runLater {
        val selection = webView.engine.executeScript("window.getSelection().toString()") as String
        destination.bufferedWriter().use { out ->
          out.write(selection)
        }
      }
    }
  }

  override fun acceptType(type: String): Boolean {
    return  type == "HTML" || type == "URL"
  }

  override fun clear() {
    Platform.runLater {
      val webView = jfxPanel.scene?.root as? WebView
      webView?.engine?.loadContent("")
    }
  }

  /** Adds into jfxPanel actionMap new actions - CopySelected and SelectAll. */
  private fun setupActions(jfxPanel: JFXPanel, webView: WebView) {

    DataManager.registerDataProvider(jfxPanel) { dataId ->
      if (PlatformDataKeys.COPY_PROVIDER.`is`(dataId)) WebViewCopyProvider(webView) else null
    }

    val actionNameSelect = "WEBVIEW_SELECT_ALL"
    val actionSelect = object : AbstractAction(actionNameSelect) {
      override fun actionPerformed(e: ActionEvent) {
        Platform.runLater {

          try {
            webView.engine.executeScript("var range, selection;\n" +
                                         "var doc = document.body;\n" +
                                         "selection = window.getSelection();\n" +
                                         "range = document.createRange();\n" +
                                         "range.selectNodeContents(doc);\n" +
                                         "selection.removeAllRanges();\n" +
                                         "selection.addRange(range);")
          }
          catch (e: Exception) {
            e.printStackTrace()
          }
        }
      }
    }
    jfxPanel.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_DOWN_MASK), actionNameSelect)
    jfxPanel.actionMap.put(actionNameSelect, actionSelect)
  }

  /**
   * Called when document successfully loaded, calculates the real height of html and calls onHeightCalculated listener.
   */
  private fun notifySize(webView: WebView) {

    val heightText = webView.engine.executeScript(   // Some modification, which gives moreless the same result than the original
      "var body = document.body,"
      + "html = document.documentElement;"
      + "Math.max( body.scrollHeight, body.offsetHeight, html.clientHeight, html.scrollHeight , html.offsetHeight );"
    ).toString()

    SwingUtilities.invokeLater {
      onHeightCalculated?.invoke(heightText.toInt())
    }
  }

  /** Listener on click on hyperlinks in rendered HTML. We need it to open links in system browser.*/
  private fun addClickListener(webView: WebView) {

    val doc = webView.engine.document

    val nodeList = doc.getElementsByTagName("a")
    for (i in 0 until nodeList.length) {
      val node = nodeList.item(i)
      val eventTarget = node as EventTarget
      eventTarget.addEventListener("click", { evt ->
        val target = evt.currentTarget
        val anchorElement = target as HTMLAnchorElement

        Platform.runLater { BrowserUtil.browse(anchorElement.href) }

        evt.preventDefault()
      }, false)
    }
  }

  override fun addData(data: String, type: String) {
    val isUrl = data.startsWith("file://") || data.startsWith("http://") || data.startsWith("https://")
    Platform.setImplicitExit(false)
    Platform.runLater {

      if (jfxPanel.scene == null) {
        webView = WebView()
        webView.isContextMenuEnabled = false
        webView.prefHeight = -1.0

        webView.engine.loadWorker.stateProperty().addListener { _, _, newState ->
          if (newState == Worker.State.SUCCEEDED) {
            notifySize(webView)
            addClickListener(webView)
          }
        }

        val scene = Scene(webView)
        jfxPanel.scene = scene

        // Fix of WebView crash on linux when we are trying to drag any content in WV (There is no crash when we are trying to select region
        // but it is not easy to detect context of drag and therefore we will cancel all drag attempts on linux)
        // https://youtrack.jetbrains.com/issue/JBR-1349
        if (SystemInfo.isLinux) {
          webView.addEventFilter(javafx.scene.input.MouseEvent.ANY) { event ->
            if (event.eventType == javafx.scene.input.MouseEvent.MOUSE_DRAGGED) {
              event.consume()
            }
          }
        }

        // Hack to make it possible to use Ctrl+A, Ctrl+C in javaFx web view.
        // - We are adding into jfxPanel.actionMap new actions with Ctrl+A, Ctrl+C
        // - Inside actions we are calling js to perform operations
        // - In setupFocusListener we are setting custom eventDispatcher which catches keyboard events before editor and transfers to our component.
        setupActions(jfxPanel, webView)

        //toolbarPane.updateChildrenBounds()
      }
      else {
        webView = jfxPanel.scene.root as WebView
      }
      if (isUrl) {
        webView.engine.load(data)
      }
      else {
        webView.engine.loadContent("<head><style>" + GithubMarkdownCss.css + " </style></head><body>" + data + "</body>")
      }
    }
  }

  // For HTML component no need to scroll to top, because it is not scrolling to end.
  override fun scrollToTop() {}

  override fun getCollapsedDescription(): String {
    return "html output"
  }
}