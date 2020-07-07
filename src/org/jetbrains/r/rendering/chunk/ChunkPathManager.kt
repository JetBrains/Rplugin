/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.application.PathManager
import com.intellij.psi.PsiElement
import java.lang.Integer.toHexString
import java.nio.file.Paths

object ChunkPathManager {

  fun getCacheDirectory(psi: PsiElement): String? {
    val path = psi.containingFile?.virtualFile?.path ?: return null
    val chunkText = psi.parent?.text ?: return null
    return Paths.get(getDirectoryForPath(path), toHexString(chunkText.hashCode())).toString()
  }

  fun getDirectoryForPath(path: String): String =
    Paths.get(PathManager.getSystemPath(), "rplugin", "cache", "chunk-output", toHexString(path.hashCode())).toString()

  fun getImagesDirectory(psi: PsiElement): String? {
    return findInCache(psi, "images")
  }

  fun getExternalImagesDirectory(psi: PsiElement): String? {
    return findInCache(psi, "external-images")
  }

  fun getHtmlDirectory(psi: PsiElement): String? {
    return getCacheDirectory(psi)
  }

  fun getHtmlLibrariesDirectory(psi: PsiElement): String? {
    return findInCache(psi, "lib")
  }

  fun getDataDirectory(psi: PsiElement): String? {
    return findInCache(psi, "data")
  }

  fun getNestedDirectories(psi: PsiElement): List<String> {
    val paths = listOf(
      getImagesDirectory(psi),
      getExternalImagesDirectory(psi),
      getHtmlLibrariesDirectory(psi),
      getDataDirectory(psi)
    )
    return paths.filterNotNull()
  }

  fun getOutputFile(psi: PsiElement): String? {
    return findInCache(psi, "output.json")
  }

  private fun findInCache(psi: PsiElement, name: String): String? {
    return getCacheDirectory(psi)?.let { cacheDirectory ->
      Paths.get(cacheDirectory, name).toString()
    }
  }
}