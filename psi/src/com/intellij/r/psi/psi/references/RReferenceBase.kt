/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi.references

import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.r.psi.classes.s4.RS4SourceManager
import com.intellij.r.psi.classes.s4.classInfo.RS4ComplexSlotPomTarget
import com.intellij.r.psi.classes.s4.classInfo.RSkeletonS4ClassPomTarget
import com.intellij.r.psi.classes.s4.classInfo.RSkeletonS4SlotPomTarget
import com.intellij.r.psi.classes.s4.classInfo.RStringLiteralPomTarget
import com.intellij.r.psi.classes.s4.context.RS4ContextProvider
import com.intellij.r.psi.classes.s4.context.methods.RS4SetGenericFunctionNameContext
import com.intellij.r.psi.classes.s4.context.methods.RS4SetMethodFunctionNameContext
import com.intellij.r.psi.console.runtimeInfo
import com.intellij.r.psi.packages.RPackage
import com.intellij.r.psi.psi.RPsiUtil
import com.intellij.r.psi.psi.RSkeletonParameterPomTarget
import com.intellij.r.psi.psi.api.RCallExpression
import com.intellij.r.psi.psi.api.RIdentifierExpression
import com.intellij.r.psi.psi.api.RNamedArgument
import com.intellij.r.psi.psi.api.RParameter
import com.intellij.r.psi.psi.api.RPsiElement
import com.intellij.r.psi.psi.api.RStringLiteralExpression
import com.intellij.r.psi.rinterop.RSourceFileManager
import com.intellij.r.psi.skeleton.RSkeletonFileType
import com.intellij.util.IncorrectOperationException

abstract class RReferenceBase<T : RPsiElement>(protected val psiElement: T) : PsiPolyVariantReference {

  @Throws(IncorrectOperationException::class)
  override fun bindToElement(element: PsiElement) = null

  override fun isReferenceTo(element: PsiElement): Boolean {
    if (element is PomTargetPsiElement &&
        element.target is RStringLiteralPomTarget &&
        psiElement is RStringLiteralExpression &&
        RS4ContextProvider.getS4Context((element.target as RStringLiteralPomTarget).literal, RS4SetGenericFunctionNameContext::class) != null &&
        RS4ContextProvider.getS4Context(psiElement, RS4SetMethodFunctionNameContext::class) != null
      ) {
      return true
    }
    return when (val resolve = resolve()) {
      is PomTargetPsiElement -> {
        if (element !is RPsiElement) return false
        if (resolve.isEquivalentTo(element)) return true
        val target = resolve.target
        if (target is RStringLiteralPomTarget && element is PomTargetPsiElement) {
          val elementTarget = element.target
          if (elementTarget is RStringLiteralPomTarget &&
              RS4ContextProvider.getS4Context(elementTarget.literal, RS4SetGenericFunctionNameContext::class) != null) {
            val context = RS4ContextProvider.getS4Context(target.literal, RS4SetMethodFunctionNameContext::class)
            if (context != null && target.literal.name == elementTarget.literal.name) return true
          }
        }
        return when {
          target is RS4ComplexSlotPomTarget -> {
            val defIdentifier =
              if (target.slotDefinition is RNamedArgument) target.slotDefinition.nameIdentifier
              else target.slotDefinition
            element === defIdentifier
          }
          element.containingFile?.virtualFile != null &&
          RSourceFileManager.isTemporary(element.containingFile.virtualFile) -> {
            target is RSkeletonParameterPomTarget &&
            element is RIdentifierExpression &&
            element.parent is RParameter &&
            element.name == target.name
          }
          RS4SourceManager.isS4ClassSourceElement(element) -> {
            when (target) {
              is RSkeletonS4SlotPomTarget -> {
                return RPsiUtil.getNamedArgumentByNameIdentifier(element) != null
              }
              is RSkeletonS4ClassPomTarget -> {
                val className = (element.containingFile.firstChild as RCallExpression).argumentList.expressionList.first()
                return element == className
              }
              else -> false
            }
          }
          else -> false
        }
      }
      is PsiNameIdentifierOwner -> resolve === element || resolve.identifyingElement === element
      else -> resolve === element
    }
  }

  override fun isSoft() = false

  override fun getCanonicalText() = element.text!!

  override fun getElement() = psiElement

  override fun getRangeInElement() = psiElement.node.textRange.shiftRight(-psiElement.node.startOffset)

  override fun handleElementRename(newElementName: String): PsiElement? = null

  override fun resolve(): PsiElement? {
    val results = multiResolve(false)
    return if (results.size == 1) results.first().element else null
  }

  override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
    return ResolveCache.getInstance(psiElement.project).resolveWithCaching(this, Resolver(), false, incompleteCode)
  }

  /**
   * @return false if all resolve targets are package members and these packages are not loaded
   */
  fun areTargetsLoaded(incompleteCode: Boolean) : Boolean {
    val runtimeInfo = psiElement.containingFile?.runtimeInfo ?: return true
    return multiResolve(incompleteCode).all { result ->
      val name = findPackageNameByResolveResult(result) ?: return@all true
      runtimeInfo.loadedPackages.keys.contains(name)
    }
  }

  protected abstract fun multiResolveInner(incompleteCode: Boolean): Array<ResolveResult>

  private class Resolver<T : RPsiElement> : ResolveCache.PolyVariantResolver<RReferenceBase<T>> {
    override fun resolve(reference: RReferenceBase<T>, incompleteCode: Boolean): Array<ResolveResult> {
      val resolveResults = reference.multiResolveInner(incompleteCode)
      val element = reference.psiElement
      return RResolver.sortValidResolveResults(element, element.containingFile.runtimeInfo, resolveResults)
    }
  }

  companion object {
    fun findPackageNameByResolveResult(result: ResolveResult): String? =
      result.element?.containingFile?.takeIf { it.fileType == RSkeletonFileType }?.let { RPackage.getOrCreateRPackageBySkeletonFile(it)?.name }
  }
}