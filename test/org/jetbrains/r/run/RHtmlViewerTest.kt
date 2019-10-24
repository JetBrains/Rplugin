/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run

import junit.framework.TestCase
import org.jetbrains.r.console.UpdateViewerHandler
import org.jetbrains.r.run.viewer.RViewerState
import org.jetbrains.r.run.viewer.RViewerUtils
import java.util.concurrent.TimeoutException

class RHtmlViewerTest : RProcessHandlerBaseTestCase() {
  private lateinit var viewerHandler: UpdateViewerHandler

  @Volatile
  private lateinit var currentUrl: String

  private val listener = object : RViewerState.Listener {
    override fun onCurrentChange(newUrl: String) {
      currentUrl = newUrl
    }

    override fun onReset() {
      // Nothing to do here
    }
  }

  override fun setUp() {
    super.setUp()

    // Setup custom "default" browser
    // which should be used for URLs starting with either 'https:' or 'http:'.
    // That means all web request will be redirected to R stdout
    rInterop.executeCode("options(browser = function(url) { print(url) })")

    // UpdateViewerHandler will setup custom "non-default" browser.
    // From now all local URLs will be written to traced tmp file
    // and then sent to listeners of current RViewerState instance
    val viewerState = RViewerUtils.createViewerState()
    viewerState.addListener(listener)
    viewerHandler = UpdateViewerHandler(rInterop, viewerState)
    currentUrl = ""
  }

  override fun tearDown() {
    // Switch back to custom "default" browser
    rInterop.htmlViewerReset()
    super.tearDown()
  }

  fun testOneFileUrl() {
    browseLocalUrl("no-such-url")
  }

  fun testTwoFileUrls() {
    browseLocalUrl("no-such-url1")
    browseLocalUrl("no-such-url2")
  }

  fun testAbsoluteLocalUrl() {
    browseLocalUrl("/tmp/rplugin/urls.txt")
  }

  fun testQualifiedLocalUrl() {
    browseLocalUrl("file:///tmp/rplugin/urls.txt")
  }

  fun testWwwUrl() {
    // Looks like it should be open in web browser but RStudio doesn't think so.
    // Thus our custom browsing function should treat it as a local URL
    browseLocalUrl("www.google.com")
  }

  fun testComUrl() {
    // The same as in 'testWwwUrl'
    browseLocalUrl("google.com")
  }

  fun testHttpsUrl() {
    browseWebUrl("https://google.com")
  }

  fun testHttpUrl() {
    browseWebUrl("http://google.com")
  }

  private fun browseLocalUrl(expectedUrl: String) {
    execute("browseURL('$expectedUrl')")
    val actualUrl = waitForUrl()
    TestCase.assertEquals(expectedUrl, actualUrl)
  }

  private fun browseWebUrl(expectedUrl: String) {
    val output = execute("browseURL('$expectedUrl')", true)
    TestCase.assertNotNull(output)
    output?.let {
      val actualUrl = it.substring(5, it.length - 2)  // e.g. "[1] 'https://google.com'\n"
      TestCase.assertEquals(expectedUrl, actualUrl)
    }
  }

  private fun execute(command: String, returnOutput: Boolean = false): String? {
    val result = rInterop.executeCode(command)
    viewerHandler.onCommandExecuted()
    return if (returnOutput) result.stdout else null
  }

  private fun waitForUrl(): String {
    val start = System.currentTimeMillis()
    while (currentUrl == "") {
      if (System.currentTimeMillis() - start > TIMEOUT) {
        throw TimeoutException("Waiting for URL for $TIMEOUT ms")
      }
      Thread.sleep(20L)
    }
    val url = currentUrl
    currentUrl = ""
    return url
  }

  companion object {
    private const val TIMEOUT = 5000L
  }
}