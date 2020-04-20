/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen

import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.r.RFileType
import javax.swing.Icon

object RoxygenFileType : LanguageFileType(RoxygenLanguage.INSTANCE) {

  override fun getName(): String = "Roxygen file"

  override fun getDescription(): String = "Roxygen language"

  override fun getDefaultExtension(): String = RFileType.defaultExtension

  override fun getIcon(): Icon? = null

}