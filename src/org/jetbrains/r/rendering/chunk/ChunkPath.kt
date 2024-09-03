/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.notebooks.visualization.NotebookCellLines
import com.intellij.notebooks.visualization.getText
import org.jetbrains.r.editor.ui.psiFile
import java.lang.Integer.toHexString
import java.nio.file.Paths

data class ChunkPath(val path: String, val chunkText: String) {

  fun getCacheDirectory(): String =
    Paths.get(getDirectoryForPath(path), toHexString(chunkText.hashCode())).toString()

  fun getImagesDirectory(): String =
    findInCache("images")

  fun getExternalImagesDirectory(): String =
    findInCache("external-images")

  fun getHtmlDirectory(): String =
    getCacheDirectory()

  private fun getHtmlLibrariesDirectory(): String =
    findInCache("lib")

  fun getDataDirectory(): String =
    findInCache( "data")

  fun getNestedDirectories(): List<String> = listOf(
    getImagesDirectory(),
    getExternalImagesDirectory(),
    getHtmlLibrariesDirectory(),
    getDataDirectory()
  )

  fun getOutputFile(): String =
    findInCache("output.json")

  private fun findInCache(name: String): String =
    Paths.get(getCacheDirectory(), name).toString()

  companion object {
    fun getDirectoryForPath(path: String): String =
      Paths.get(PathManager.getSystemPath(), "rplugin", "cache", "chunk-output", toHexString(path.hashCode())).toString()

    fun create(editor: Editor, interval: NotebookCellLines.Interval): ChunkPath? {
      val path = editor.psiFile?.virtualFile?.path ?: return null
      val chunkText = editor.document.getText(interval)
      return ChunkPath(path, chunkText)
    }

    fun create(psi: PsiElement): ChunkPath? {
      val path = psi.containingFile?.virtualFile?.path ?: return null
      val chunkText = psi.parent?.text ?: return null
      return ChunkPath(path, chunkText)
    }
  }
}
