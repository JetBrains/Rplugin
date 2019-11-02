// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.configuration

import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.layout.*
import javax.swing.JComponent
import javax.swing.JLabel

class RRunConfigurationForm {
  private val scriptPathField = TextFieldWithBrowseButton()
  private val rootPanel = panel {
    row(JLabel("Script:")) {
      scriptPathField()
    }
  }

  val panel: JComponent
    get() = rootPanel

  var scriptPath: String
    get() = FileUtil.toSystemIndependentName(scriptPathField.text.trim { it <= ' ' })
    set(value) {
      scriptPathField.text = FileUtil.toSystemDependentName(value)
    }
}
