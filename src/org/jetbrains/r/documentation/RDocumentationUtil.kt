/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2011 Holger Brandl
 *
 * This code is licensed under BSD. For details see
 * http://www.opensource.org/licenses/bsd-license.php
 */

package org.jetbrains.r.documentation

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.rinterop.RInterop
import java.nio.charset.Charset

object RDocumentationUtil {
  fun makeElementForText(rInterop: RInterop, text: String, url: String): PsiElement {
    return RElementFactory.createRPsiElementFromText(rInterop.project, "text").also {
      it.putCopyableUserData(ELEMENT_TEXT) { convertHelpPage(text, url) }
      it.putCopyableUserData(ELEMENT_NAVIGATE_BY_LINK) { link -> makeElementForLink(rInterop, link) }
    }
  }

  private fun makeElementForLink(rInterop: RInterop, url: String): PsiElement {
    return RElementFactory.createRPsiElementFromText(rInterop.project, "text").also {
      it.putCopyableUserData(ELEMENT_TEXT) {
        rInterop.httpdRequest(url)?.let { response ->
          convertHelpPage(String(response.content, Charset.defaultCharset()), response.url)
        } ?: "$url not found"
      }
      it.putCopyableUserData(ELEMENT_NAVIGATE_BY_LINK) { link -> makeElementForLink(rInterop, link) }
    }
  }

  internal fun getTextFromElement(element: PsiElement) = element.getCopyableUserData(ELEMENT_TEXT)?.invoke()

  internal fun navigateByLinkFromElement(element: PsiElement, link: String) =
    element.getCopyableUserData(ELEMENT_NAVIGATE_BY_LINK)?.invoke(link)

  private fun convertHelpPage(text: String, url: String): String {
    val urlComponents = url.substringBefore('?').substringAfter("://")
      .substringAfter('/', "").substringBeforeLast('/', "")
      .split('/')
    return Regex("href\\s*=\\s*\"([^\"]*)\"").replace(text) {
      val link = it.groupValues[1]
      val result = when {
        link.startsWith('#') -> url
        link.startsWith("http://127.0.0.1") -> {
          "psi_element:///" + link.substringAfter("://").substringAfter('/', "")
        }
        "://" in link -> link
        link.startsWith('/') -> "psi_element://" + link
        else -> {
          var parentCount = 0
          while (link.startsWith("../", parentCount * 3)) ++parentCount
          val prefix = urlComponents.dropLast(parentCount).joinToString("/")
          "psi_element:///" + prefix + (if (prefix.isEmpty()) "" else "/") + link.drop(parentCount * 3)
        }
      }
      "href=\"$result\""
    }
      .let { Regex("<a href.*<img.*></a>").replace(it, "") }
      .let { Regex("<img.*>").replace(it, "") }
  }

private val ELEMENT_TEXT = Key<() -> String>("org.jetbrains.r.documentation.ElementText")
  private val ELEMENT_NAVIGATE_BY_LINK = Key<(String) -> PsiElement>("org.jetbrains.r.documentation.ElementNavigate")
}
