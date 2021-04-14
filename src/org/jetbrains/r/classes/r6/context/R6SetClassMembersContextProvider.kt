/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.r6.context

import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.classes.common.context.ILibraryClassContext
import org.jetbrains.r.classes.r6.R6ClassInfoUtil
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RMemberExpression
import org.jetbrains.r.psi.api.RPsiElement
import org.jetbrains.r.psi.isFunctionFromLibrary
import org.jetbrains.r.psi.references.RSearchScopeUtil
import org.jetbrains.r.psi.stubs.classes.LibraryClassNameIndexProvider

sealed class R6SetClassMembersContext : ILibraryClassContext {
  override val functionName = R6ClassInfoUtil.functionSet
}

// MyClass$set(<caret>)
// MyClass$set("<caret>")
data class R6SetClassMembersContextVisibility(override val originalElement: RPsiElement,
                                              override val functionCall: RCallExpression,
                                              override val argumentInfo: RArgumentInfo) : R6SetClassMembersContext()

// MyClass$set(visibility, <caret>)
// MyClass$set("visibility", <caret>)
data class R6SetClassMembersContextName(override val originalElement: RPsiElement,
                                              override val functionCall: RCallExpression,
                                              override val argumentInfo: RArgumentInfo) : R6SetClassMembersContext()

class R6SetClassMembersContextProvider : R6ContextProvider<R6SetClassMembersContext>() {
  override fun getContext(element: RPsiElement): R6SetClassMembersContext? {
    return CachedValuesManager.getCachedValue(element) {
      CachedValueProvider.Result.create(getR6ContextInner(element), element)
    }
  }

  override fun getR6ContextInner(element: RPsiElement): R6SetClassMembersContext? {
    val parentCall = PsiTreeUtil.getParentOfType(element, RCallExpression::class.java) ?: return null
    if (!isFromR6Library(parentCall)) return null

    // todo finish with suggesting `MyClass$set(...)`
    val parentArgumentInfo = RParameterInfoUtil.getArgumentInfo(parentCall) ?: return null

    return null
  }

  private fun isFromR6Library(rCallExpression: RCallExpression) : Boolean {
    val memberExpression = rCallExpression.expression as? RMemberExpression ?: return false
    if (!memberExpression.lastChild.textMatches(R6ClassInfoUtil.functionSet)) return false

    val r6ClassIdentifier = memberExpression.firstChild
    val cachedClasses = LibraryClassNameIndexProvider.R6ClassNameIndex.findClassInfos(r6ClassIdentifier.text, memberExpression.project, RSearchScopeUtil.getScope(rCallExpression))
    return (!cachedClasses.isNullOrEmpty())
  }
}