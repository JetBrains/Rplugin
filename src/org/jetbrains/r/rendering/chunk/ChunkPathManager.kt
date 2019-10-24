/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package icons.org.jetbrains.r.rendering.chunk

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
    return Paths.get(getCacheDirectory(psi) ?: return null, "images").toString()
  }

  fun getHtmlDirectory(psi: PsiElement): String? {
    return getCacheDirectory(psi)
  }

  fun getDataDirectory(psi: PsiElement): String? {
    return Paths.get(getCacheDirectory(psi) ?: return null, "data").toString()
  }

  fun getOutputFile(psi: PsiElement): String? {
    return Paths.get(getCacheDirectory(psi) ?: return null, "output.json").toString()
  }
}