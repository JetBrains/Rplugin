/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.IconUtil
import com.intellij.util.ui.ImageUtil
import com.intellij.util.ui.UIUtil
import org.intellij.datavis.r.inlays.*
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.jetbrains.plugins.notebooks.editor.use
import org.jetbrains.r.rmarkdown.RMarkdownFileType
import org.jetbrains.r.rmarkdown.R_FENCE_ELEMENT_TYPE
import org.jetbrains.r.run.graphics.RGraphicsDevice
import org.jetbrains.r.run.graphics.RPlotUtil
import org.jetbrains.r.run.graphics.RSnapshot
import java.awt.Image
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.Future
import javax.imageio.ImageIO
import javax.swing.Icon

class ChunkDescriptorProvider : InlayDescriptorProvider {
  override fun getInlayDescriptor(editor: Editor): InlayElementDescriptor? {
    return runReadAction {
      if (isNewMode(editor)) return@runReadAction null
      val psiFile = PsiDocumentManager.getInstance(editor.project ?: return@runReadAction null).getPsiFile(editor.document)
      if (psiFile?.virtualFile?.fileType is RMarkdownFileType) RMarkdownInlayDescriptor(psiFile) else null
    }
  }

  companion object {
    fun isNewMode(editor: Editor): Boolean =
      Registry.`is`("r.interpreter.useOutputInlays")
  }
}

class RMarkdownInlayDescriptor(override val psiFile: PsiFile) : InlayElementDescriptor {
  override fun cleanup(psi: PsiElement): Future<Void> =
    RMarkdownInlayDescriptor.cleanup(psi)

  override fun isInlayElement(psi: PsiElement): Boolean {
    return psi is LeafPsiElement && psi.elementType == MarkdownTokenTypes.CODE_FENCE_END &&
           (psi.prevSibling?.prevSibling?.let { it is LeafPsiElement && it.elementType === R_FENCE_ELEMENT_TYPE } == true)
  }

  override fun getInlayOutputs(psi: PsiElement): List<InlayOutput> =
    RMarkdownInlayDescriptor.getInlayOutputs(psi)


  companion object {
    fun cleanup(psi: PsiElement): Future<Void> {
      val cacheDirectory = ChunkPathManager.getCacheDirectory(psi)!!
      return FileUtil.asyncDelete(File(cacheDirectory))
    }

    fun getInlayOutputs(psi: PsiElement): List<InlayOutput> =
      getImages(psi) + getUrls(psi) + getTables(psi) + getOutputs(psi)

    fun getImages(psi: PsiElement): List<InlayOutput> {
      return getImageFilesOrdered(psi).map { imageFile ->
        val text = imageFile.absolutePath
        val preview = ImageIO.read(imageFile)?.let { image ->
          IconUtil.createImageIcon(RPlotUtil.fitTheme(createPreview(image)))
        }
        InlayOutput(text, "IMG", preview = preview)
      }
    }

    private fun createPreview(image: BufferedImage): BufferedImage {
      val previewHeight = PREVIEW_ICON_HEIGHT
      val previewWidth = PREVIEW_ICON_WIDTH
      return ImageUtil.createImage(previewWidth, previewHeight, BufferedImage.TYPE_INT_RGB).also { preview ->
        preview.createGraphics().use {
          it.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
          it.drawImage(image, 0, 0, previewWidth, previewHeight, null)
        }
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
                    preview = createIconWithText("HTML"))
      } ?: emptyList()
    }

    fun getTables(psi: PsiElement): List<InlayOutput> {
      val dataDirectory = ChunkPathManager.getDataDirectory(psi) ?: return emptyList()
      return getFilesByExtension(dataDirectory, ".csv")?.map { csv ->
        InlayOutput(csv.readText(),
                    "TABLE",
                    preview = createIconWithText("Table"))
      } ?: emptyList()
    }

    fun getOutputs(psi: PsiElement): List<InlayOutput> {
      return ChunkPathManager.getOutputFile(psi)?.let { File(it) }?.takeIf { it.exists() }?.let {
        listOf(InlayOutput(it.absolutePath,
                           "Output",
                           preview = createIconWithText("Console")))
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

private fun createIconWithText(text: String): Icon {
  val image = ImageUtil.createImage( PREVIEW_ICON_WIDTH, PREVIEW_ICON_HEIGHT, BufferedImage.TYPE_INT_RGB)
  val rectangle = Rectangle(0, 0, PREVIEW_ICON_WIDTH, PREVIEW_ICON_HEIGHT)
  image.createGraphics().use { graphics ->
    graphics.color = EditorColorsManager.getInstance().globalScheme.defaultBackground
    graphics.fill(rectangle)
    graphics.color = EditorColorsManager.getInstance().globalScheme.defaultForeground
    UIUtil.drawCenteredString(graphics, rectangle, text, true, true)
  }
  return IconUtil.createImageIcon(image as Image)
}

private val PREVIEW_ICON_WIDTH = JBUIScale.scale(150)
private val PREVIEW_ICON_HEIGHT = JBUIScale.scale(100)

private val RMARKDOWN_CHUNK = TextAttributesKey.createTextAttributesKey("RMARKDOWN_CHUNK")
