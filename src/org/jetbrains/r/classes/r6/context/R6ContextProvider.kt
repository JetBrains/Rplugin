/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.r6.context

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.r.classes.common.context.LibraryClassContext
import org.jetbrains.r.classes.common.context.LibraryContextProvider

abstract class R6ContextProvider : LibraryContextProvider<LibraryClassContext>() {
  companion object {
    private val EP_NAME: ExtensionPointName<LibraryContextProvider<out LibraryClassContext>> =
      ExtensionPointName.create("com.intellij.r6ContextProvider")
  }
}