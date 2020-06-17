// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndex
import icons.RIcons

object RFileType : LanguageFileType(RLanguage.INSTANCE) {

  override fun getName(): String = "R"

  override fun getDescription(): String = "R scripts"

  override fun getDefaultExtension(): String = "R"

  override fun getIcon() = RIcons.R_logo_16

  var INPUT_FILTER: FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(this)

  val DOT_R_EXTENSION = "." + defaultExtension
}
