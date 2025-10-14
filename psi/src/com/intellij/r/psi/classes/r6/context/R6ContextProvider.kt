/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.classes.r6.context

import com.intellij.r.psi.psi.api.RPsiElement
import java.lang.reflect.ParameterizedType

abstract class R6ContextProvider<T : R6Context> {
  abstract fun getR6ContextInner(element: RPsiElement): T?
  abstract fun getContext(element: RPsiElement): T?

  @Suppress("UNCHECKED_CAST")
  private val contextClass = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<T>

  companion object {
    fun getProviders(): List<R6ContextProvider<out R6Context>> = listOf(
      R6CreateClassContextProvider(),
      R6SetClassMembersContextProvider()
    )

    fun getR6Context(element: RPsiElement): R6Context? {
      return getR6Context(element, R6Context::class.java)
    }

    fun <T : R6Context> getR6Context(element: RPsiElement, vararg searchedContexts: Class<out T>): T? {
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