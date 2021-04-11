package org.jetbrains.r.classes.s4.context

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.r.classes.common.context.ILibraryClassContext
import org.jetbrains.r.psi.api.RPsiElement
import java.lang.reflect.ParameterizedType

abstract class RS4ContextProvider<T : ILibraryClassContext> {

  abstract fun getContext(element: RPsiElement): T?

  @Suppress("UNCHECKED_CAST")
  private val contextClass = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<T>

  companion object {
    private val EP_NAME: ExtensionPointName<RS4ContextProvider<out ILibraryClassContext>> =
      ExtensionPointName.create("com.intellij.rS4ContextProvider")

    fun getProviders(): List<RS4ContextProvider<out ILibraryClassContext>> = EP_NAME.extensionList

    fun getS4Context(element: RPsiElement): ILibraryClassContext? {
      return getS4Context(element, ILibraryClassContext::class.java)
    }

    fun <T : ILibraryClassContext> getS4Context(element: RPsiElement, vararg searchedContexts: Class<out T>): T? {
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