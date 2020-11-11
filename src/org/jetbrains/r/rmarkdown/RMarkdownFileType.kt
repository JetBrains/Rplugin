/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileType.CharsetHintSupplied.CharsetHint.*
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VirtualFile
import icons.RIcons
import org.jetbrains.annotations.NonNls
import java.nio.charset.StandardCharsets
import javax.swing.Icon

object RMarkdownFileType : LanguageFileType(RMarkdownLanguage), FileType.CharsetHintSupplied {
  override fun getName() = "RMarkdown"
  @NonNls
  override fun getDescription() = "R Markdown"
  override fun getDefaultExtension() = "Rmd"
  override fun getIcon(): Icon = RIcons.RMarkdown
  override fun getCharset(file: VirtualFile, content: ByteArray): String = CharsetToolkit.UTF8
  override fun getCharsetHint(): FileType.CharsetHintSupplied.CharsetHint = ForcedCharset(StandardCharsets.UTF_8)
}