/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.bin

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

object RBinFileType : FileType {
  const val DOT_R_BIN_EXTENSION = "IdeaRBin"

  override fun getDefaultExtension(): String = DOT_R_BIN_EXTENSION

  override fun getIcon(): Icon? = null

  override fun getCharset(file: VirtualFile, content: ByteArray): String? = null

  override fun getName(): String = DOT_R_BIN_EXTENSION

  override fun getDescription(): String = "Binary summary format for Idea R Plugin"

  override fun isBinary(): Boolean = true

  override fun isReadOnly(): Boolean = true
}
