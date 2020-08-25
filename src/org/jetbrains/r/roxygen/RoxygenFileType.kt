/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen

import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.r.RBundle
import javax.swing.Icon

object RoxygenFileType : LanguageFileType(RoxygenLanguage.INSTANCE) {

  override fun getName(): String = "Roxygen"

  override fun getDescription(): String = RBundle.message("fileType.roxygen.documentation.description")

  override fun getDefaultExtension(): String = "roxygen"

  override fun getIcon(): Icon? = null

}