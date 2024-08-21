/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.visualization.inlays.components

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.util.ui.JBUI
import java.util.*

/** github-markdown.css as String, dependent form current IDE theme. */
object GithubMarkdownCss {

  private val bright: String by lazy { changeFontSize(getResourceAsString("/css/github-intellij.css")) }
  private val dark: String by lazy { changeFontSize(getResourceAsString("/css/github-darcula.css")) }

  fun getDarkOrBright(): String =
    if (EditorColorsManager.getInstance().isDarkEditor) dark
    else bright

  private fun getResourceAsString(resource: String): String {
    val inputStream = GithubMarkdownCss::class.java.classLoader.getResourceAsStream(resource)
    val scanner = Scanner(inputStream!!).useDelimiter("\\A")
    return if (scanner.hasNext()) scanner.next() else ""
  }

  private fun changeFontSize(css: String): String {
    val index = css.indexOf("font-size: 16px;")
    if (index == -1) {
      return css
    }
    return css.replaceRange(index + 11, index + 13, JBUI.scaleFontSize(14f).toString())
  }
}