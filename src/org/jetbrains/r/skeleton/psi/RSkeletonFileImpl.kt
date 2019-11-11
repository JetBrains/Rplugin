/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.skeleton.psi

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.file.PsiBinaryFileImpl
import com.intellij.psi.impl.source.PsiFileWithStubSupport
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.stubs.StubTree
import com.intellij.psi.stubs.StubTreeLoader
import com.intellij.reference.SoftReference
import org.jetbrains.r.RLanguage
import org.jetbrains.r.psi.RRecursiveElementVisitor
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.skeleton.RSkeletonFileType
import java.lang.ref.Reference


class RSkeletonFileImpl(viewProvider: FileViewProvider)
  : PsiBinaryFileImpl(viewProvider.manager as PsiManagerImpl, viewProvider), PsiFileWithStubSupport {
  @Volatile
  private var stub: Reference<StubTree>? = null
  private val stubLock = Object()

  @Volatile
  private var mirrorData: Reference<MirrorData>? = null
  private val mirrorLock = Object()

  override fun getContainingFile(): PsiFile {
    if (!isValid) throw PsiInvalidElementAccessException(this)
    return this
  }

  override fun getLanguage(): Language {
    return RLanguage.INSTANCE
  }

  override fun toString(): String {
    return "RSkeletonFile:$name"
  }

  override fun accept(visitor: PsiElementVisitor) {
    visitor.visitFile(this)
  }

  override fun getStubTree(): StubTree {
    ApplicationManager.getApplication().assertReadAccessAllowed()
    SoftReference.dereference(stub)?.let { return it }

    // build newStub out of lock to avoid deadlock
    var newStubTree = StubTreeLoader.getInstance().readOrBuild(project, virtualFile, this) as StubTree?
    if (newStubTree == null) {
      newStubTree = StubTree(RSkeletonFileStub())
    }
    synchronized(stubLock) {
      SoftReference.dereference(stub)?.let { return it }
      val fileStub = newStubTree.root as PsiFileStubImpl<PsiFile>
      fileStub.setPsi(this)
      stub = SoftReference(newStubTree)
    }
    return newStubTree
  }

  override fun getFileType(): FileType {
    return RSkeletonFileType
  }

  private fun calculateTopLevelNameMap(mirror: PsiFile): Map<String, RAssignmentStatement> {
    val result = HashMap<String, RAssignmentStatement>()
    mirror.accept(object : RRecursiveElementVisitor() {
      override fun visitAssignmentStatement(assignment: RAssignmentStatement) {
        val isTopLevelAssign = assignment.getParent() == mirror
        if (isTopLevelAssign) {
          result[assignment.name] = assignment
        }
      }
    })
    return result
  }

  data class MirrorData(val file: PsiFile, val topLevelNameMap: Map<String, RAssignmentStatement>)
}
