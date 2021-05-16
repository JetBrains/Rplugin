/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.s4.context

import com.intellij.openapi.util.Key
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.r.classes.common.context.LibraryClassContext
import org.jetbrains.r.classes.s4.context.setClass.RS4SetClassTypeContextProvider
import org.jetbrains.r.classes.common.context.ILibraryClassContext
import org.jetbrains.r.classes.s4.context.methods.RS4SetGenericProvider
import org.jetbrains.r.classes.s4.context.methods.RS4SetGenericValueClassesContext
import org.jetbrains.r.classes.s4.context.methods.RS4SetMethodProvider
import org.jetbrains.r.classes.s4.context.methods.RS4SetMethodSignatureClassNameContext
import org.jetbrains.r.classes.s4.context.setClass.RS4SetClassTypeUsageContext
import org.jetbrains.r.classes.s4.context.setClass.RS4SlotDeclarationContextProvider
import org.jetbrains.r.psi.api.RPsiElement
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

abstract class RS4ContextProvider<T : LibraryClassContext> {

  fun getContext(element: RPsiElement): T? {
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
    val newObjectProvider = RS4NewObjectContextProvider()
    val setClassTypeProvider = RS4SetClassTypeContextProvider()
    val slotDeclarationProvider = RS4SlotDeclarationContextProvider()
    val setGenericProvider = RS4SetGenericProvider()
    val setMethodProvider = RS4SetMethodProvider()

    private val allProviders = listOf(
      newObjectProvider,
      setClassTypeProvider,
      slotDeclarationProvider,
      setGenericProvider,
      setMethodProvider,
    )

    fun getS4Context(element: RPsiElement): LibraryClassContext? = getS4Context(element, LibraryClassContext::class)

    fun <T : LibraryClassContext> getS4Context(element: RPsiElement, vararg searchedContexts: KClass<out T>): T? {
      return getS4Context(element, *searchedContexts.map { it.java }.toTypedArray())
    }

    @JvmStatic
    fun <T : LibraryClassContext> getS4Context(element: RPsiElement, vararg searchedContexts: Class<out T>): T? {
      for (provider in allProviders) {
        if (searchedContexts.any { provider.contextClass.isAssignableFrom(it) || it.isAssignableFrom(provider.contextClass) }) {
          val s4Context = provider.getContext(element) ?: continue
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