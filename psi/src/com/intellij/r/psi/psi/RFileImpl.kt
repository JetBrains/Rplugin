// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi

import com.intellij.codeInsight.controlflow.Instruction
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.DummyBlockType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.r.psi.RFileType
import com.intellij.r.psi.psi.api.RCallExpression
import com.intellij.r.psi.psi.api.RFile
import com.intellij.r.psi.psi.cfg.LocalAnalysisResult
import com.intellij.r.psi.psi.cfg.RControlFlow
import com.intellij.r.psi.psi.cfg.analyzeLocals
import com.intellij.r.psi.psi.cfg.buildControlFlow
import com.intellij.r.psi.psi.references.IncludedSources
import com.intellij.r.psi.psi.references.analyseIncludedSources
import com.intellij.util.Processor


open class RFileImpl(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, RFileType.language), RFile {
  override val controlFlow: RControlFlow
    get() = CachedValuesManager.getCachedValue(this) {
      CachedValueProvider.Result.create(buildControlFlow(this), this)
    }

  override val localAnalysisResult: LocalAnalysisResult
    get() = CachedValuesManager.getCachedValue(this) {
      CachedValueProvider.Result.create(analyzeLocals().getValue(this), this)
    }

  override val includedSources: Map<Instruction, IncludedSources>
    get() = CachedValuesManager.getCachedValue(this) { CachedValueProvider.Result(analyseIncludedSources(), this) }

  private var myImports: CachedValue<List<RCallExpression>>? = null

  override fun getFileType(): FileType {
    return RFileType
  }

  override fun toString(): String {
    return "RFile:$name"
  }

  override fun accept(visitor: PsiElementVisitor) {
    visitor.visitFile(this)
  }

  override fun getImportExpressions(element: PsiElement): List<RCallExpression> {
    if (myImports == null) {
      myImports = CachedValuesManager.getManager(project).createCachedValue(
        { CachedValueProvider.Result.create(findImports(), this@RFileImpl) }, false)
    }

    return myImports!!.value
  }

  private fun findImports(): List<RCallExpression> {

    val result = ArrayList<RCallExpression>()
    processChildrenDummyAware(this, object : Processor<PsiElement> {
      override fun process(psiElement: PsiElement): Boolean {
        if (RPsiUtil.isImportStatement(psiElement)) {
          result.add(psiElement as RCallExpression)
        }
        processChildrenDummyAware(psiElement, this)

        return true
      }
    })
    return result
  }

  private fun processChildrenDummyAware(element: PsiElement, processor: Processor<PsiElement>): Boolean {
    return object : Processor<PsiElement> {
      override fun process(psiElement: PsiElement): Boolean {
        var child: PsiElement? = psiElement.firstChild
        while (child != null) {
          if (child is DummyBlockType.DummyBlock) {
            if (!process(child)) return false
          }
          else if (!processor.process(child)) return false
          child = child.nextSibling
        }
        return true
      }
    }.process(element)
  }
}
