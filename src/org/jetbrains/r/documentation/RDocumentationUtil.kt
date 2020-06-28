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

object RDocumentationUtil {
  fun makeElementForText(rInterop: RInterop, httpdResponse: RInterop.HttpdResponse): PsiElement {
    return RElementFactory.createRPsiElementFromText(rInterop.project, "text").also {
      it.putCopyableUserData(ELEMENT_TEXT) { convertHelpPage(httpdResponse) }
    }
  }

  internal fun getTextFromElement(element: PsiElement) = element.getCopyableUserData(ELEMENT_TEXT)?.invoke()

  internal fun convertHelpPage(httpdResponse: RInterop.HttpdResponse): String {
    var (text, url) = httpdResponse
    val bodyStart = text.indexOf("<body>")
    val bodyEnd = text.lastIndexOf("</body>")
    if (bodyStart != -1 && bodyEnd != -1) text = text.substring(bodyStart + "<body>".length, bodyEnd)
    val urlComponents = url.substringBefore('?').substringAfter("://")
      .substringAfter('/', "").substringBeforeLast('/', "")
      .split('/')
    return Regex("href\\s*=\\s*\"([^\"]*)\"").replace(text) {
      val link = it.groupValues[1]
      val result = when {
        link.startsWith('#') -> "psi_element://$url"
        link.startsWith("http://127.0.0.1") -> {
          "psi_element:///" + link.substringAfter("://").substringAfter('/', "")
        }
        "://" in link -> link
        link.startsWith('/') -> "psi_element://$link"
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
}
