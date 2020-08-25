/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.IconUtil
import org.intellij.datavis.r.inlays.InlayDescriptorProvider
import org.intellij.datavis.r.inlays.InlayDimensions
import org.intellij.datavis.r.inlays.InlayElementDescriptor
import org.intellij.datavis.r.inlays.InlayOutput
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.jetbrains.r.rendering.chunk.RunChunkNavigator.createRunChunkActionGroup
import org.jetbrains.r.rmarkdown.RMarkdownFileType
import org.jetbrains.r.rmarkdown.R_FENCE_ELEMENT_TYPE
import org.jetbrains.r.run.graphics.RGraphicsDevice
import org.jetbrains.r.run.graphics.RSnapshot
import java.awt.Color
import java.awt.Font
import java.io.File
import java.util.concurrent.Future
import javax.swing.ImageIcon

class ChunkDescriptorProvider : InlayDescriptorProvider {
  override fun getInlayDescriptor(editor: Editor): InlayElementDescriptor? {
    return runReadAction {
      val psiFile = PsiDocumentManager.getInstance(editor.project ?: return@runReadAction null).getPsiFile(editor.document)
       if (psiFile?.virtualFile?.fileType is RMarkdownFileType) RMarkdownInlayDescriptor(psiFile, editor) else null
    }
  }
}

class RMarkdownInlayDescriptor(override val psiFile: PsiFile, private val editor: Editor) : InlayElementDescriptor {
  private val highlighters: ArrayList<RangeHighlighter> = ArrayList()

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

  override fun onUpdateHighlighting(toolbarElements: Collection<PsiElement>) {
    val markupModel = editor.markupModel
    highlighters.forEach { markupModel.removeHighlighter(it) }
    highlighters.clear()
    editor.colorsScheme.getAttributes(RMARKDOWN_CHUNK).backgroundColor?.let { backgroundColor ->
      toolbarElements.forEach { fillChunkArea(it.parent.textRange, backgroundColor, markupModel) }
    }
  }

  override fun getToolbarActions(psi: PsiElement): ActionGroup? = if (isChunkFenceLang(psi)) createRunChunkActionGroup(psi) else null

  override fun isToolbarActionElement(psi: PsiElement): Boolean = isChunkFenceLang(psi)

  private fun fillChunkArea(textRange: TextRange, backgroundColor: Color, markupModel: MarkupModel) {
    highlighters.add(markupModel.addRangeHighlighter(textRange.startOffset,
                                                     textRange.endOffset,
                                                     HighlighterLayer.ADDITIONAL_SYNTAX + 1,
                                                     TextAttributes(null,
                                                                    backgroundColor,
                                                                    null,
                                                                    EffectType.ROUNDED_BOX,
                                                                    Font.PLAIN),
                                                     HighlighterTargetArea.LINES_IN_RANGE))
  }

  companion object {
    fun getImages(psi: PsiElement): List<InlayOutput> {
      return getImageFilesOrdered(psi).map { imageFile ->
        val bytes = FileUtil.loadFileBytes(imageFile)
        val imageIcon = ImageIcon(bytes)
        val preview = IconUtil.scale(imageIcon, null, InlayDimensions.lineHeight * 4.0f / imageIcon.iconHeight)
        val text = imageFile.absolutePath
        InlayOutput(text, "IMG", preview = preview)
      }
    }

    private fun getImageFilesOrdered(psi: PsiElement): List<File> {
      val snapshots = getSnapshots(psi)?.map { ExternalImage(it.file, it.number, 0) } ?: emptyList()
      val external = getExternalImages(psi) ?: emptyList()
      val triples = snapshots + external
      val sorted = triples.sortedWith(compareBy(ExternalImage::major, ExternalImage::minor))
      return sorted.map { it.file }
    }

    private fun getSnapshots(psi: PsiElement): List<RSnapshot>? {
      return ChunkPathManager.getImagesDirectory(psi)?.let { directory ->
        RGraphicsDevice.fetchLatestNormalSnapshots(File(directory))
      }
    }

    private fun getExternalImages(psi: PsiElement): List<ExternalImage>? {
      return ChunkPathManager.getExternalImagesDirectory(psi)?.let { directory ->
        File(directory).listFiles()?.mapNotNull { file ->
          ExternalImage.from(file)
        }
      }
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

private data class ExternalImage(
  val file: File,
  val major: Int,
  val minor: Int
) {
  companion object {
    private const val IMAGE_MAGIC = "image"

    fun from(file: File): ExternalImage? {
      val parts = file.nameWithoutExtension.split('_')
      if (parts.size != 3 || parts[0] != IMAGE_MAGIC) {
        return null
      }
      val major = parts[1].toInt()
      val minor = parts[2].toInt()
      return ExternalImage(file, major, minor)
    }
  }
}

private val RMARKDOWN_CHUNK = TextAttributesKey.createTextAttributesKey("RMARKDOWN_CHUNK")
