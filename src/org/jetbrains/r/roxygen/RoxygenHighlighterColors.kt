/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

private typealias Colors = DefaultLanguageHighlighterColors

val COMMENT_TEXT = create("ROXYGEN_COMMENT_TEXT", Colors.DOC_COMMENT)

val TAG_NAME = create("ROXYGEN_TAG_NAME", Colors.DOC_COMMENT_TAG)
val PARAMETER = create("ROXYGEN_PARAMETER", Colors.DOC_COMMENT_TAG_VALUE)

val LINK_DESTINATION = create("ROXYGEN_LINK_DESTINATION", Colors.STATIC_METHOD)
val AUTOLINK = create("ROXYGEN_AUTOLINK", Colors.STATIC_METHOD)
val HELP_PAGE_LINK = create("ROXYGEN_HELP_PAGE_LINK", Colors.DOC_COMMENT_TAG_VALUE)

private fun create(externalName: String, fallback: TextAttributesKey): TextAttributesKey =
  TextAttributesKey.createTextAttributesKey(externalName, fallback)
