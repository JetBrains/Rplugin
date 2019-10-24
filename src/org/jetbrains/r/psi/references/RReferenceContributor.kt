/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.references

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.util.ProcessingContext
import com.jetbrains.extensions.python.toPsi
import org.jetbrains.r.RLanguage
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.psi.RElementFilters
import org.jetbrains.r.psi.api.RFile
import org.jetbrains.r.psi.api.RStringLiteralExpression


class RReferenceContributor : PsiReferenceContributor() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    val filePathReferenceProvider = object : PsiReferenceProvider() {
      override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val stringLiteral = element as? RStringLiteralExpression ?: return PsiReference.EMPTY_ARRAY
        val text = stringLiteral.name ?: return PsiReference.EMPTY_ARRAY
        val file = element.containingFile
        val project = element.project
        val set = object : FileReferenceSet(text, element, 1, this, true, true) {
          override fun getDefaultContexts(): MutableCollection<PsiFileSystemItem> {
            val result = super.getDefaultContexts()
            val workingDir = (file as? RFile)?.runtimeInfo?.workingDir
            if (workingDir != null) {
              val dir = LocalFileSystem.getInstance().findFileByPath(workingDir)?.toPsi(project)
              if (dir != null) {
                return result.plus(dir).toMutableList()
              }
            }
            return result
          }
        }
        return set.allReferences.filterIsInstance<PsiReference>().toTypedArray()
      }
    }

    registrar.registerReferenceProvider(
      PlatformPatterns.psiElement().withLanguage(RLanguage.INSTANCE).and(RElementFilters.STRING_FILTER),
      filePathReferenceProvider, PsiReferenceRegistrar.LOWER_PRIORITY
    )
  }
}
