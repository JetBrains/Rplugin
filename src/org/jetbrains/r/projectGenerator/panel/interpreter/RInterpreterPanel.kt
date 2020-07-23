/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.panel.interpreter

import com.intellij.openapi.ui.ValidationInfo
import org.jetbrains.r.interpreter.RInterpreterLocation
import org.jetbrains.r.projectGenerator.panel.RPanel

abstract class RInterpreterPanel : RPanel() {
  open val interpreterLocation: RInterpreterLocation? = null

  open fun validateInterpreter(): List<ValidationInfo> = emptyList()

  open fun fetchInstalledPackages(): List<String> = emptyList()
}