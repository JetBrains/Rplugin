/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.r6.context

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.r.classes.s4.context.RS4Context
import org.jetbrains.r.classes.s4.context.RS4ContextProvider
import org.jetbrains.r.psi.api.RPsiElement
import java.lang.reflect.ParameterizedType

abstract class R6ContextProvider <T: R6Context> {
  abstract fun getR6Context(element: RPsiElement): T?

  @Suppress("UNCHECKED_CAST")
  private val contextClass = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<T>

  companion object {
    private val EP_NAME: ExtensionPointName<R6ContextProvider<out R6Context>> =
      ExtensionPointName.create("com.intellij.r6ContextProvider")

    fun getProviders(): List<R6ContextProvider<out R6Context>> = EP_NAME.extensionList

    fun getR6Context(element: RPsiElement): R6Context? {
      return getR6Context(element, R6Context::class.java)
    }

    fun <T : R6Context> getR6Context(element: RPsiElement, vararg searchedContexts: Class<out T>): T? {
      for (provider in getProviders()) {
        if (searchedContexts.any { it.isAssignableFrom(provider.contextClass) }) {
          val r6Context = provider.getR6Context(element)
          if (r6Context != null) {
            @Suppress("UNCHECKED_CAST")
            return r6Context as T?
          }
        }
      }
      return null
    }
  }
}