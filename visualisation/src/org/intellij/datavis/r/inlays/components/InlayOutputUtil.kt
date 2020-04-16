/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileChooser.ex.FileChooserDialogImpl
import com.intellij.openapi.fileChooser.ex.FileSaverDialogImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.intellij.datavis.r.VisualizationBundle
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Paths
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier

object InlayOutputUtil {
  private val EXPORT_IMAGE_TITLE = VisualizationBundle.message("inlay.output.image.export.text")
  private val EXPORT_IMAGE_DESCRIPTION = VisualizationBundle.message("inlay.output.image.export.description")

  private val EXPORT_FAILURE_TITLE = VisualizationBundle.message("inlay.output.export.failure")
  private val EXPORT_FAILURE_DETAILS = VisualizationBundle.message("inlay.output.export.failure.details")
  private val EXPORT_FAILURE_DESCRIPTION = VisualizationBundle.message("inlay.output.export.failure.description")

  fun saveImageWithFileChooser(project: Project, image: BufferedImage, onSave: ((File) -> Unit)? = null) {
    chooseImageSaveLocation(project, image) { location ->
      ImageIO.write(image, location.extension, location)
      onSave?.invoke(location)
    }
  }

  private fun chooseImageSaveLocation(project: Project, image: BufferedImage, onChoose: (File) -> Unit) {
    val extensions = getAvailableFormats(image).toTypedArray()
    saveWithFileChooser(project, EXPORT_IMAGE_TITLE, EXPORT_IMAGE_DESCRIPTION, extensions, "image", false, onChoose)
  }

  private fun getAvailableFormats(image: BufferedImage): List<String> {
    val imageTypeSpecifier = ImageTypeSpecifier.createFromRenderedImage(image)
    return getAvailableFormats().filter { format ->
      imageTypeSpecifier.hasWritersFor(format)
    }
  }

  fun getAvailableFormats(): List<String> {
    return listOf("png", "jpeg", "bmp", "gif", "tiff")
  }

  private fun ImageTypeSpecifier.hasWritersFor(format: String): Boolean {
    return ImageIO.getImageWriters(this, format).asSequence().any()
  }

  fun chooseDirectory(project: Project, title: String, description: String): VirtualFile? {
    val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
      .withDescription(description)
      .withTitle(title)
    val chooser = FileChooserDialogImpl(descriptor, project)
    val toSelect = project.virtualBaseDir?.let { arrayOf(it) } ?: emptyArray()
    val choice = chooser.choose(project, *toSelect)
    return choice.firstOrNull()
  }

  fun saveWithFileChooser(
    project: Project,
    title: String,
    description: String,
    extensions: Array<String>,
    defaultName: String,
    createIfMissing: Boolean,
    onChoose: (File) -> Unit
  ) {
    val descriptor = FileSaverDescriptor(title, description, *extensions)
    val chooser = FileSaverDialogImpl(descriptor, project)
    chooser.save(project.virtualBaseDir, defaultName)?.let { fileWrapper ->
      val destination = fileWrapper.file
      try {
        checkOrCreateDestinationFile(destination, createIfMissing)
        onChoose(destination)
      } catch (e: Exception) {
        notifyExportError(e)
      }
    }
  }

  private val Project.virtualBaseDir: VirtualFile?
    get() = VfsUtil.findFile(Paths.get(basePath!!), true)

  private fun checkOrCreateDestinationFile(file: File, createIfMissing: Boolean) {
    if (!file.exists()) {
      if (!file.createNewFile()) {
        throw RuntimeException(EXPORT_FAILURE_DETAILS)
      }
      if (!createIfMissing && !file.delete()) {
        throw RuntimeException(EXPORT_FAILURE_DETAILS)
      }
    }
  }

  private fun notifyExportError(e: Exception) {
    val details = e.message?.let { ":\n$it" }
    val content = "$EXPORT_FAILURE_DESCRIPTION$details"
    Messages.showErrorDialog(content, EXPORT_FAILURE_TITLE)
  }
}
