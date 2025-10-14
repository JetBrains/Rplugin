/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi.references

import com.intellij.psi.ResolveResult
import com.intellij.r.psi.psi.api.RIdentifierExpression

class RNamespaceReference(private val element: RIdentifierExpression,
                          private val namespaceName: String) : RReferenceBase<RIdentifierExpression>(element) {
  override fun multiResolveInner(incompleteCode: Boolean): Array<ResolveResult> {
    val resultResults = ArrayList<ResolveResult>()
    RResolver.resolveWithNamespace(element.project, element.name, namespaceName, resultResults)
    return resultResults.toTypedArray()
  }
}


