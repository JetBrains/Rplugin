// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.r.psi.icons.RIcons
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndex

object RFileType : LanguageFileType(RLanguage.INSTANCE) {

  override fun getName(): String = "R"

  override fun getDescription(): String = RBundle.message("filetype.r.description")

  override fun getDefaultExtension(): String = "R"

  override fun getIcon() = RIcons.R

  var INPUT_FILTER: FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(this)

  val DOT_R_EXTENSION = ".$defaultExtension"
}
