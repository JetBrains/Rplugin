/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.s4.context

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.Key
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.r.classes.s4.context.methods.RS4SetGenericValueClassesContext
import org.jetbrains.r.classes.s4.context.methods.RS4SetMethodSignatureClassNameContext
import org.jetbrains.r.classes.s4.context.setClass.RS4SetClassTypeUsageContext
import org.jetbrains.r.psi.api.RPsiElement
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

abstract class RS4ContextProvider<T : RS4Context> {

  fun getS4Context(element: RPsiElement): T? {
    return if (element is PomTargetPsiElement) getS4ContextForPomTarget(element)
    else CachedValuesManager.getCachedValue(element, contextKey) {
      CachedValueProvider.Result.create(getS4ContextWithoutCaching(element), element)
    }
  }

  protected abstract fun getS4ContextWithoutCaching(element: RPsiElement): T?
  protected open fun getS4ContextForPomTarget(element: PomTargetPsiElement): T? = null

  @Suppress("UNCHECKED_CAST")
  private val contextClass = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<T>
  private val contextKey: Key<CachedValue<T>> = Key.create("S4_CONTEXT_PROVIDER_CACHE")

  companion object {
    private val EP_NAME:
      ExtensionPointName<RS4ContextProvider<out RS4Context>> =
      ExtensionPointName.create("com.intellij.rS4ContextProvider")

    fun getProviders(): List<RS4ContextProvider<out RS4Context>> = EP_NAME.extensionList

    fun getS4Context(element: RPsiElement): RS4Context? = getS4Context(element, RS4Context::class)

    fun <T : RS4Context> getS4Context(element: RPsiElement, vararg searchedContexts: KClass<out T>): T? {
      return getS4Context(element, *searchedContexts.map { it.java }.toTypedArray())
    }

    @JvmStatic
    fun <T : RS4Context> getS4Context(element: RPsiElement, vararg searchedContexts: Class<out T>): T? {
      for (provider in getProviders()) {
        if (searchedContexts.any { provider.contextClass.isAssignableFrom(it) || it.isAssignableFrom(provider.contextClass) }) {
          val s4Context = provider.getS4Context(element) ?: continue
          for (context in searchedContexts) {
            if (context.isInstance(s4Context)) {
              return context.cast(s4Context)
            }
          }
        }
      }
      return null
    }

    val S4_CLASS_USAGE_CONTEXTS = arrayOf(RS4NewObjectClassNameContext::class,
                                          RS4SetClassTypeUsageContext::class,
                                          RS4SetGenericValueClassesContext::class,
                                          RS4SetMethodSignatureClassNameContext::class)
  }
}