/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.documentation

import junit.framework.TestCase
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class RHttpdDocumentationTest : RProcessHandlerBaseTestCase() {
  fun testHelpRequest() {
    val (content, url) = executeHelpCommand("?print")
    TestCase.assertTrue(url.endsWith("/library/base/html/print.html"))
    TestCase.assertTrue(content.contains("print {base}"))
  }

  fun testHelpFunctionRequest() {
    val (content, url) = executeHelpCommand("help('base')")
    TestCase.assertTrue(url.endsWith("/library/base/html/base-package.html"))
    TestCase.assertTrue(content.contains("base-package {base}"))
  }

  fun testSearchHelpRequestUrl() {
    val (content, url) = executeHelpCommand("??utils")
    TestCase.assertTrue(url.contains("/doc/html/Search?"))
    TestCase.assertTrue(content.contains("utils::install.packages"))
  }

  fun testHttpdRequest() {
    val response = rInterop.httpdRequest("/library/base/html/print.html")!!
    TestCase.assertEquals("/library/base/html/print.html", response.url)
    TestCase.assertTrue(response.content.toString(Charsets.UTF_8).contains("print {base}"))
  }

  fun testHttpdRequestRedirect() {
    val response = rInterop.httpdRequest("/library/base/help/print")!!
    TestCase.assertEquals("/library/base/html/print.html", response.url)
    TestCase.assertTrue(response.content.toString(Charsets.UTF_8).contains("print {base}"))
  }

  fun testLinkTransform() {
    val url = "/a/b/c/d/e.html"
    val input = """
      <a href="https://www.google.com/">link</a>
      <a href="/x/y/z.html">link</a>
      <a href="x/y/z.html">link</a>
      <a href="../../x/y/z.html">link</a>
      <a href="xyz.html">link</a>
      <a href="http://127.0.0.1:1234/q/w/e.html">link</a>
    """.trimIndent()
    val expected = """
      <a href="https://www.google.com/">link</a>
      <a href="psi_element:///x/y/z.html">link</a>
      <a href="psi_element:///a/b/c/d/x/y/z.html">link</a>
      <a href="psi_element:///a/b/x/y/z.html">link</a>
      <a href="psi_element:///a/b/c/d/xyz.html">link</a>
      <a href="psi_element:///q/w/e.html">link</a>
    """.trimIndent()
    TestCase.assertEquals(expected, RDocumentationUtil.getTextFromElement(RDocumentationUtil.makeElementForText(rInterop, input, url)))
  }

  private data class HelpRequest(val content: String, val url: String)

  private fun executeHelpCommand(command: String): HelpRequest {
    val result = AsyncPromise<HelpRequest>()
    rInterop.addAsyncEventsListener(object : RInterop.AsyncEventsListener {
      override fun onShowHelpRequest(content: String, url: String) {
        result.setResult(HelpRequest(content, url))
        rInterop.removeAsyncEventsListener(this)
      }
    })
    rInterop.asyncEventsStartProcessing()
    rInterop.executeCode(command)
    return result.blockingGet(DEFAULT_TIMEOUT)!!
  }
}
