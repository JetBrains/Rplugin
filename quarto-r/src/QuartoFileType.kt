package com.intellij.quarto

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.quartoR.icons.QuartoRIcons
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

object QuartoFileType : LanguageFileType(QuartoLanguage) {
  override fun getName() = "Quarto"
  @NonNls
  override fun getDescription() = "Quarto"
  override fun getDefaultExtension() = "Qmd"
  override fun getIcon(): Icon = QuartoRIcons.Quarto
  override fun getCharset(file: VirtualFile, content: ByteArray): String = CharsetToolkit.UTF8
}