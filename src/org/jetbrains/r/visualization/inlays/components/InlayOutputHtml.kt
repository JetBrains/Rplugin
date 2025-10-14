package org.jetbrains.r.visualization.inlays.components

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Disposer
import com.intellij.r.psi.RBundle
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.jetbrains.r.visualization.inlays.InlayOutputData
import org.jetbrains.r.visualization.inlays.MouseWheelUtils
import java.util.function.Function
import javax.swing.SwingUtilities

class InlayOutputHtml(parent: Disposable, editor: Editor)
  : InlayOutput(editor, loadActions(SaveOutputAction.Companion.ID)), InlayOutput.WithSaveAs {

  private val jbBrowser: JBCefBrowser = JBCefBrowser().also { Disposer.register(parent, it) }
  private val heightJsCallback = JBCefJSQuery.create(jbBrowser as JBCefBrowserBase)
  private val saveJsCallback = JBCefJSQuery.create(jbBrowser as JBCefBrowserBase)
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
    return type == "HTML" || type == "URL"
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

  fun addData(data: InlayOutputData.HtmlUrl) {
    val isUrl = data.url.startsWith("file://")
    if (isUrl) {
      jbBrowser.loadURL(data.url)
    }
    else {
      // this never happens, url is always starts with `file://`
      jbBrowser.loadHTML("<head><style>" + GithubMarkdownCss.getDarkOrBright() + " </style></head><body>" + data + "</body>")
    }
    jbBrowser.jbCefClient.addLoadHandler(object : org.cef.handler.CefLoadHandlerAdapter() {
      override fun onLoadingStateChange(browser: org.cef.browser.CefBrowser?, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
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
    val title = RBundle.message("inlay.action.export.as.txt.title")
    val description = RBundle.message("inlay.action.exports.range.csv.description")
    val label = RBundle.message("inlay.action.export.as.txt.label")
    saveWithFileChooser(title, description, label, arrayOf("txt")) { destination ->
      saveJsCallback.addHandler(object : Function<String, JBCefJSQuery.Response> {
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
