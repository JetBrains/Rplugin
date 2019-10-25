// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova


/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider

class LiveTemplateProvider : DefaultLiveTemplatesProvider {

  override fun getDefaultLiveTemplateFiles(): Array<String> {
    return DEFAULT_TEMPLATES
  }

  override fun getHiddenLiveTemplateFiles(): Array<String>? {
    return null
  }

  companion object {
    private val DEFAULT_TEMPLATES = arrayOf("/liveTemplates/rtemplates")
  }
}
