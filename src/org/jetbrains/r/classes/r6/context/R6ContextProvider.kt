/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.r6.context

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.r.classes.common.context.ILibraryClassContext
import org.jetbrains.r.psi.api.RPsiElement
import java.lang.reflect.ParameterizedType

abstract class R6ContextProvider<T : ILibraryClassContext> {
  abstract fun getR6ContextInner(element: RPsiElement): T?
  abstract fun getContext(element: RPsiElement): T?

  @Suppress("UNCHECKED_CAST")
  private val contextClass = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<T>

  companion object {
    fun getProviders(): List<R6ContextProvider<out ILibraryClassContext>> = listOf(
      R6CreateClassContextProvider(),
      R6SetClassMembersContextProvider()
    )

    fun getR6Context(element: RPsiElement): ILibraryClassContext? {
      return getR6Context(element, ILibraryClassContext::class.java)
    }

    fun <T : ILibraryClassContext> getR6Context(element: RPsiElement, vararg searchedContexts: Class<out T>): T? {
      for (provider in getProviders()) {
        if (searchedContexts.any { it.isAssignableFrom(provider.contextClass) }) {
          val s4Context = provider.getContext(element)
          if (s4Context != null) {
            @Suppress("UNCHECKED_CAST")
            return s4Context as T?
          }
        }
      }
      return null
    }
  }
}