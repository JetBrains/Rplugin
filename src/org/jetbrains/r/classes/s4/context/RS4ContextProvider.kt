package org.jetbrains.r.classes.s4.context

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.r.classes.common.context.LibraryClassContext
import org.jetbrains.r.classes.common.context.LibraryContextProvider

abstract class RS4ContextProvider: LibraryContextProvider<LibraryClassContext>(){
  companion object {
    private val EP_NAME: ExtensionPointName<LibraryContextProvider<out LibraryClassContext>> =
      ExtensionPointName.create("com.intellij.rS4ContextProvider")
  }
}