/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.r.editor.ui.psiFile
import org.jetbrains.r.visualization.RNotebookCellLines.Interval
import org.jetbrains.r.visualization.ui.getText
import java.lang.Integer.toHexString
import java.nio.file.Path
import java.nio.file.Paths

data class ChunkPath(val path: String, val chunkText: String) {
  fun getCacheDirectory(): Path =
    getDirectoryForPath(path).resolve( toHexString(chunkText.hashCode()))

  fun getImagesDirectory(): Path =
    findInCache("images")

  fun getExternalImagesDirectory(): Path =
    findInCache("external-images")

  fun getHtmlDirectory(): Path =
    getCacheDirectory()

  private fun getHtmlLibrariesDirectory(): Path =
    findInCache("lib")

  fun getDataDirectory(): Path =
    findInCache( "data")

  fun getNestedDirectories(): List<Path> = listOf(
    getImagesDirectory(),
    getExternalImagesDirectory(),
    getHtmlLibrariesDirectory(),
    getDataDirectory(),
  )

  fun getOutputFile(): Path =
    findInCache("output.json")

  private fun findInCache(name: String): Path =
    getCacheDirectory().resolve(name)

  companion object {
    fun getDirectoryForPath(path: String): Path =
      Paths.get(PathManager.getSystemPath(), "rplugin", "cache", "chunk-output", toHexString(path.hashCode()))

    fun create(editor: Editor, interval: Interval): ChunkPath? {
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
