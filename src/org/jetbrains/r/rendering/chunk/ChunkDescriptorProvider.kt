/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.IconUtil
import icons.org.jetbrains.r.rendering.chunk.ChunkPathManager
import org.intellij.datavis.inlays.InlayDescriptorProvider
import org.intellij.datavis.inlays.InlayDimensions
import org.intellij.datavis.inlays.InlayElementDescriptor
import org.intellij.datavis.inlays.InlayOutput
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.jetbrains.r.rendering.chunk.RunChunkNavigator.createRunChunkActionGroup
import org.jetbrains.r.rmarkdown.RMarkdownFileType
import org.jetbrains.r.rmarkdown.R_FENCE_ELEMENT_TYPE
import java.io.File
import java.util.concurrent.Future
import javax.swing.ImageIcon

class ChunkDescriptorProvider : InlayDescriptorProvider {
  override fun getInlayDescriptor(editor: Editor): InlayElementDescriptor? {
    return runReadAction {
      val psiFile = PsiDocumentManager.getInstance(editor.project ?: return@runReadAction null).getPsiFile(editor.document)
       if (psiFile?.virtualFile?.fileType is RMarkdownFileType) RMarkdownInlayDescriptor(psiFile) else null
    }
  }
}

class RMarkdownInlayDescriptor(override val psiFile: PsiFile) : InlayElementDescriptor {
  override fun cleanup(psi: PsiElement): Future<Void> {
    val cacheDirectory = ChunkPathManager.getCacheDirectory(psi)!!
    return FileUtil.asyncDelete(File(cacheDirectory))
  }

  override fun isInlayElement(psi: PsiElement): Boolean {
    return psi is LeafPsiElement && psi.elementType == MarkdownTokenTypes.CODE_FENCE_END &&
           (psi.prevSibling?.prevSibling?.let { it is LeafPsiElement && it.elementType === R_FENCE_ELEMENT_TYPE } == true)
  }

  override fun getInlayOutputs(psi: PsiElement): List<InlayOutput> {
    return getImages(psi) + getUrls(psi) + getTables(psi) + getOutputs(psi)
  }

  override fun getToolbarActions(psi: PsiElement): ActionGroup? = if (isChunkFenceLang(psi)) createRunChunkActionGroup(psi) else null

  override fun isToolbarActionElement(psi: PsiElement): Boolean = isChunkFenceLang(psi)

  companion object {
    fun getImages(psi: PsiElement): List<InlayOutput> {

      val imagesDirectory = ChunkPathManager.getImagesDirectory(psi) ?: return emptyList()
      return getFilesByExtension(imagesDirectory, ".png")?.map { png ->
        val bytes = FileUtil.loadFileBytes(png)
        val imageIcon = ImageIcon(bytes, "preview")
        val preview = IconUtil.scale(imageIcon, null, InlayDimensions.lineHeight * 4.0f / imageIcon.iconHeight)
        val text = png.absolutePath
        InlayOutput(text, "IMG", preview = preview)
      } ?: emptyList()
    }

    private val preferredWidth
      get() = (InlayDimensions.lineHeight * 8.0f).toInt()

    fun getUrls(psi: PsiElement): List<InlayOutput> {
      val imagesDirectory = ChunkPathManager.getHtmlDirectory(psi) ?: return emptyList()
      return getFilesByExtension(imagesDirectory, ".html")?.map { html ->
        InlayOutput("file://" + html.absolutePath.toString(),
                    "URL",
                    title = "HTML",
                    preferredWidth = preferredWidth)
      } ?: emptyList()
    }

    fun getTables(psi: PsiElement): List<InlayOutput> {
      val dataDirectory = ChunkPathManager.getDataDirectory(psi) ?: return emptyList()
      return getFilesByExtension(dataDirectory, ".csv")?.map { csv ->
        InlayOutput(csv.readText(),
                    "TABLE",
                    title = "Table",
                    preferredWidth = preferredWidth)
      } ?: emptyList()
    }

    fun getOutputs(psi: PsiElement): List<InlayOutput> {
      return ChunkPathManager.getOutputFile(psi)?.let { File(it) }?.takeIf { it.exists() }?.let {
        listOf(InlayOutput(it.absolutePath,
                           "Output",
                           title = "R Console",
                           preferredWidth = preferredWidth))
      } ?: emptyList()
    }

    private fun getFilesByExtension(imagesDirectory: String, extension: String): Array<File>? =
      File(imagesDirectory).takeIf { it.exists() }?.listFiles { _, name ->
        name.endsWith(extension)
      }?.apply { sortBy { it.lastModified() } }
  }
}