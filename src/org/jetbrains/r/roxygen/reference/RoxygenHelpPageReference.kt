/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.psi.api.RFile
import org.jetbrains.r.psi.references.RResolver
import org.jetbrains.r.roxygen.RoxygenUtil
import org.jetbrains.r.roxygen.isNamespaceAccess
import org.jetbrains.r.roxygen.psi.api.RoxygenIdentifierExpression
import org.jetbrains.r.roxygen.psi.api.RoxygenNamespaceAccessExpression

class RoxygenHelpPageReference(element: RoxygenIdentifierExpression, textRange: TextRange)
  : RoxygenReferenceBase<RoxygenIdentifierExpression>(element, textRange), PsiPolyVariantReference {

  private val namespaceName: String? by lazy { (element.parent as? RoxygenNamespaceAccessExpression)?.namespaceName }

  override fun resolve(): PsiElement? {
    val results = multiResolve(false)
    return if (results.size == 1) results.first().element else null
  }

  override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
    return ResolveCache.getInstance(psiElement.project).resolveWithCaching(this, Resolver(), false, incompleteCode)
  }

  private class Resolver : ResolveCache.PolyVariantResolver<RoxygenHelpPageReference> {
    override fun resolve(reference: RoxygenHelpPageReference, incompleteCode: Boolean): Array<ResolveResult> {
      val psiElement = reference.psiElement
      val resolveResults = ArrayList<ResolveResult>()
      if (psiElement.isNamespaceAccess()) {
        reference.namespaceName?.let { RResolver.resolveWithNamespace(psiElement.project, psiElement.name, it, resolveResults) }
      } else {
        val psiComment = RoxygenUtil.findHostComment(psiElement.parent)
        psiComment?.let {
          RResolver.resolveInFilesOrLibrary(it, psiElement.name, resolveResults)
          val runtimeInfo = (psiComment.containingFile as? RFile)?.runtimeInfo
          return RResolver.sortValidResolveResults(psiComment, runtimeInfo, resolveResults.toTypedArray())
        }
      }
      return resolveResults.toTypedArray()
    }
  }
}