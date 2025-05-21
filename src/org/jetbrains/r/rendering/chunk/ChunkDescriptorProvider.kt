/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.IconUtil
import com.intellij.util.ui.ImageUtil
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.r.RBundle
import org.jetbrains.r.run.graphics.RGraphicsDevice
import org.jetbrains.r.run.graphics.RPlotUtil
import org.jetbrains.r.run.graphics.RSnapshot
import org.jetbrains.r.settings.RGraphicsSettings
import org.jetbrains.r.visualization.inlays.InlayOutputData
import org.jetbrains.r.visualization.ui.use
import java.awt.Image
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.swing.Icon
import kotlin.io.path.*

object RMarkdownInlayDescriptor {
  suspend fun cleanup(chunkPath: ChunkPath): Unit =
    withContext(Dispatchers.IO) {
      FileUtilRt.deleteRecursively(chunkPath.getCacheDirectory())
    }

  fun getInlayOutputs(chunkPath: ChunkPath, project: Project): List<InlayOutputData> =
    getInlayOutputs(chunkPath, RGraphicsSettings.isDarkModeEnabled(project))

  private fun getInlayOutputs(chunkPath: ChunkPath, isDarkModeEnabled: Boolean): List<InlayOutputData> =
    getImages(chunkPath, isDarkModeEnabled) + getUrls(chunkPath) + getTables(chunkPath) + getTextOutputs(chunkPath)

  private fun getImages(chunkPath: ChunkPath, isDarkModeEnabled: Boolean): List<InlayOutputData.Image> {
    return getImageFilesOrdered(chunkPath).map { path ->
      val preview = ImageIO.read(path.toFile())?.let { image ->
        IconUtil.createImageIcon(RPlotUtil.fitTheme(createPreview(image), isDarkModeEnabled))
      }
      InlayOutputData.Image(path, preview = preview)
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

  private fun getImageFilesOrdered(chunkPath: ChunkPath): List<Path> {
    val snapshots = getSnapshots(chunkPath)?.map { ExternalImage(it.file, it.number, 0) } ?: emptyList()
    val external = getExternalImages(chunkPath)
    val triples = snapshots + external
    val sorted = triples.sortedWith(compareBy(ExternalImage::major, ExternalImage::minor))
    return sorted.map { it.file }
  }

  private fun getSnapshots(chunkPath: ChunkPath): List<RSnapshot>? {
    val directory = chunkPath.getImagesDirectory()
    return RGraphicsDevice.fetchLatestNormalSnapshots(directory)
  }

  private fun getExternalImages(chunkPath: ChunkPath): List<ExternalImage> {
    val directory = chunkPath.getExternalImagesDirectory()
    return directory.listDirectoryEntries().mapNotNull { file ->
      ExternalImage.from(file)
    }
  }

  private fun getUrls(chunkPath: ChunkPath): List<InlayOutputData.HtmlUrl> {
    val imagesDirectory = chunkPath.getHtmlDirectory()
    return getFilesByExtension(imagesDirectory, ".html").map { html ->
      InlayOutputData.HtmlUrl("file://" + html.toAbsolutePath().toString(),
                              preview = createIconWithText(RBundle.message("rmarkdown.output.html.title")))
    }
  }

  private fun getTables(chunkPath: ChunkPath): List<InlayOutputData.CsvTable> {
    val dataDirectory = chunkPath.getDataDirectory()
    return getFilesByExtension(dataDirectory, ".csv").map { csv ->
      InlayOutputData.CsvTable(csv.readText(),
                               preview = createIconWithText(RBundle.message("rmarkdown.output.table.title")))
    }
  }

  fun getTextOutputs(chunkPath: ChunkPath): List<InlayOutputData.TextOutput> {
    return chunkPath.getOutputFile().takeIf { it.exists() }?.let {
      listOf(InlayOutputData.TextOutput(it.toAbsolutePath(),
                                        preview = createIconWithText(RBundle.message("rmarkdown.output.console.title"))))
    } ?: emptyList()
  }

  private fun getFilesByExtension(imagesDirectory: Path, extension: String): List<Path> {
    if (!imagesDirectory.exists()) return emptyList()

    return imagesDirectory.listDirectoryEntries()
      .filter { it.name.endsWith(extension) }
      .sortedBy { it.getLastModifiedTime() }
  }
}

private data class ExternalImage(
  val file: Path,
  val major: Int,
  val minor: Int,
) {
  companion object {
    private const val IMAGE_MAGIC = "image"

    fun from(file: Path): ExternalImage? {
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
  val image = ImageUtil.createImage(PREVIEW_ICON_WIDTH, PREVIEW_ICON_HEIGHT, BufferedImage.TYPE_INT_RGB)
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
