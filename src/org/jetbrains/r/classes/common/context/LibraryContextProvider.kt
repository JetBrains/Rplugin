/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.common.context

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.r.psi.api.RPsiElement
import java.lang.reflect.ParameterizedType

abstract class LibraryContextProvider <T: LibraryClassContext> {
  abstract fun getContext(element: RPsiElement): T?

  @Suppress("UNCHECKED_CAST")
  private val contextClass = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<T>

  companion object {
    private val EP_NAME: ExtensionPointName<LibraryContextProvider<out LibraryClassContext>> =
      ExtensionPointName.create("com.intellij.libraryContextProvider")

    fun getProviders(): List<LibraryContextProvider<out LibraryClassContext>> = EP_NAME.extensionList

    fun getContext(element: RPsiElement): LibraryClassContext? {
      return getContext(element, LibraryClassContext::class.java)
    }

    fun <T : LibraryClassContext> getContext(element: RPsiElement, vararg searchedContexts: Class<out T>): T? {
      for (provider in getProviders()) {
        if (searchedContexts.any { it.isAssignableFrom(provider.contextClass) }) {
          val context = provider.getContext(element)
          if (context != null) {
            @Suppress("UNCHECKED_CAST")
            return context as T?
          }
        }
      }
      return null
    }
  }
}