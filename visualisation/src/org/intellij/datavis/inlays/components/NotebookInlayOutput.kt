/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.inlays.components

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.CopyProvider
import com.intellij.ide.DataManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
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
import com.intellij.ui.RelativeFont
import com.intellij.ui.components.JBScrollBar
import com.intellij.util.ui.TextTransferable
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import org.intellij.datavis.inlays.MouseWheelUtils
import org.w3c.dom.events.EventTarget
import org.w3c.dom.html.HTMLAnchorElement
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.SwingUtilities.invokeLater
import kotlin.math.min

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
class NotebookInlayOutput(private val project: Project, private val parent: Disposable) : NotebookInlayState(), ToolBarProvider {

  companion object {
    private const val RESIZE_TASK_NAME = "Resize graphics"
    private const val RESIZE_TASK_IDENTITY = "Resizing graphics"
    private const val RESIZE_TIME_SPAN = 500

    private val monospacedFont = RelativeFont.NORMAL.family(Font.MONOSPACED)
    private val outputFont = monospacedFont.derive(UIUtil.getLabelFont().deriveFont(UIUtil.getFontSize(UIUtil.FontSize.SMALL)))
  }

  abstract inner class Output(parent: Disposable) {

    protected val toolbarPane = ToolbarPane()

    fun getComponent(): Component {
      return toolbarPane
    }

    /** Clears view, removes text/html. */
    abstract fun clear()

    abstract fun addData(data: String)
    abstract fun scrollToTop()
    abstract fun getCollapsedDescription(): String

    abstract fun saveAs()

    /**
     * Inlay component can can adjust itself to fit the Output.
     * We need callback because text output can return height immediately,
     * but Html output can return height only delayed, from it's Platform.runLater.
     */
    var onHeightCalculated: ((height: Int) -> Unit)? = null

    private val disposable: Disposable = Disposer.newDisposable()

    init {
      Disposer.register(parent, disposable)
    }

    open fun addToolbar() {
      toolbarPane.toolbarComponent = createToolbar()
    }

    protected fun createToolbar(): JComponent {
      val actionGroup = DefaultActionGroup(createSaveAsAction(), createClearAction())
      val toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, actionGroup, true)
      return toolbar.component.apply {
        isOpaque = false
        background = Color(0, 0, 0, 0)
      }
    }
  }

  inner class OutputImg(parent: Disposable) : Output(parent) {
    private val graphicsPanel = GraphicsPanel(project)
    private val queue = MergingUpdateQueue(RESIZE_TASK_NAME, RESIZE_TIME_SPAN, true, null, project)

    private var imagePath: String? = null

    init {
      toolbarPane.centralComponent = graphicsPanel.component
      graphicsPanel.component.addComponentListener(object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent?) {
          scheduleResizing()
        }
      })
    }

    override fun addData(data: String) {
      imagePath = data
      graphicsPanel.showImage(File(data))
      onHeightCalculated?.invoke(graphicsPanel.maximumSize?.height ?: 0)
      scheduleResizing()
    }

    override fun clear() {
    }

    override fun scrollToTop() {
    }

    override fun getCollapsedDescription(): String {
      return "foo"
    }

    override fun saveAs() {
      imagePath?.let { path ->
        val title = "Export image"
        val description = "Save image to disk"
        saveWithFileChooser(title, description, "png", "snapshot.png") { destination ->
          Files.copy(Paths.get(path), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
      }
    }

    private fun scheduleResizing() {
      queue.queue(object : Update(RESIZE_TASK_IDENTITY) {
        override fun run() {
          val oldSize = graphicsPanel.imageSize
          val newSize = graphicsPanel.imageComponentSize
          if (oldSize != newSize) {
            resize(newSize)
          }
        }
      })
    }

    private fun resize(newSize: Dimension) {
      imagePath?.let { path ->
        GraphicsManager.getInstance()?.resizeImage(project, path, newSize) { imageFile ->
          imagePath = imageFile.absolutePath
          ApplicationManager.getApplication().invokeLater {
            graphicsPanel.showImage(imageFile)
          }
        }
      }
    }
  }

  inner class OutputText(parent: Disposable) : Output(parent) {

    private val console = ColoredTextConsole(project, viewer = true)

    init {
      Disposer.register(parent, console)
      toolbarPane.centralComponent = console.component
      (console.editor as EditorImpl).backgroundColor = UIUtil.getPanelBackground()
      (console.editor as EditorImpl).scrollPane.border = null
      MouseWheelUtils.wrapMouseWheelListeners((console.editor as EditorImpl).scrollPane)

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

    override fun addToolbar() {
      val toolbar = createToolbar()
      (console.editor as EditorImpl).scrollPane.verticalScrollBar.add(JBScrollBar.LEADING, toolbar)
    }

    override fun clear() {
      console.clear()
    }

    override fun addData(data: String) {
      File(data).takeIf { it.exists() && it.extension == "json" }?.let {file ->
        Gson().fromJson<List<ProcessOutput>>(file.readText(), object : TypeToken<List<ProcessOutput>>(){}.type).forEach {
          console.addData(it.text, it.kind)
        }
      } ?: console.addData(data, ProcessOutputType.STDOUT)
      ApplicationManager.getApplication().invokeLater {
        console.flushDeferredText()
        onHeightCalculated?.invoke(console.preferredSize.height)
      }
    }

    override fun scrollToTop() {
      console.scrollTo(0)
    }

    override fun getCollapsedDescription(): String {
      return console.text.substring(0, min(console.text.length, 60)) + " ....."
    }

    override fun saveAs() {
      val title = "Export as txt"
      val description = "Export console content to text file"
      saveWithFileChooser(title, description, "txt", "output.txt") { destination ->
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
  inner class OutputHtml(parent: Disposable) : Output(parent) {

    private val jfxPanel = JFXPanel()
    private lateinit var webView: WebView

    init {
      MouseWheelUtils.wrapMouseWheelListeners(jfxPanel)

      toolbarPane.centralComponent = jfxPanel
      jfxPanel.putClientProperty("AuxEditorComponent", true)
    }

    override fun saveAs() {
      val title = "Export as txt"
      val description = "Exports the selected range or whole table if nothing is selected as csv or tsv file"
      saveWithFileChooser(title, description, "txt", "output.txt") { destination ->
        Platform.runLater {
          val selection = webView.engine.executeScript("window.getSelection().toString()") as String
          destination.bufferedWriter().use { out ->
            out.write(selection)
          }
        }
      }
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

      invokeLater {
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

    override fun addData(data: String) {
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

  private var output: Output? = null

  override fun createActions(): List<AnAction> {
    return listOf(createSaveAsAction())
  }

  private fun createSaveAsAction(): AnAction {
    return object : DumbAwareAction("Save As", "Save as", AllIcons.Actions.Menu_saveall) {
      override fun actionPerformed(e: AnActionEvent) {
        output?.saveAs()
      }
    }
  }

  private fun createClearAction(): AnAction {
    return object : DumbAwareAction("Clear", "Clear output", AllIcons.Actions.GC) {
      override fun actionPerformed(e: AnActionEvent) {
        clearAction.invoke()
      }
    }
  }

  private fun addTextOutput() = createOutput { OutputText(parent) }

  private fun addHtmlOutput() = createOutput { OutputHtml(parent) }

  private fun addImgOutput() = createOutput { OutputImg(parent) }

  private inline fun createOutput(constructor: (Disposable) -> Output) = constructor(parent).apply { setupOutput(this) }

  private fun setupOutput(output: Output) {
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

  private fun saveWithFileChooser(title: String, description: String, extension: String, defaultName: String, onChoose: (File) -> Unit) {
    val descriptor = FileSaverDescriptor(title, description, extension)
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
    val notification = Notification("NotebookInlayOutput", "Export failure", content, NotificationType.ERROR)
    notification.notify(project)
  }

  private var addToolbar = false

  fun addToolbar() {
    addToolbar = true
    output?.addToolbar()
  }

  fun setError(data: String) {

    if (output == null || output is OutputHtml) {
      addTextOutput()
    }

    output!!.clear()
    output!!.scrollToTop()
  }

  fun addData(type: String, data: String) {
    when (type) {
      "HTML", "URL" -> output?.takeIf { it is OutputHtml } ?: addHtmlOutput()
      "IMG" -> output?.takeIf { it is OutputImg } ?: addImgOutput()
      else -> output?.takeIf { it is OutputText } ?: addTextOutput()
    }.addData(data)
  }

  override fun  clear() {
    output?.clear()
  }

  override fun getCollapsedDescription(): String {
    return if (output == null) "" else output!!.getCollapsedDescription()
  }
}