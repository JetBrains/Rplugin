/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.skeleton.psi

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.testFramework.ReadOnlyLightVirtualFile
import com.intellij.util.IncorrectOperationException
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.r.RLanguage
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RExpression
import org.jetbrains.r.psi.api.RFunctionExpression
import org.jetbrains.r.psi.stubs.RAssignmentStub
import org.jetbrains.r.skeleton.RSkeletonFileDecompiler
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.util.concurrent.TimeoutException

class RSkeletonAssignmentStatement(private val myStub: RSkeletonAssignmentStub) : RSkeletonBase(), RAssignmentStatement {
  override fun getMirror() = null

  private var decompiled: Reference<ReadOnlyLightVirtualFile>? = null

  override fun getParent(): PsiElement = myStub.getParentStub().getPsi()

  override fun setName(name: String): PsiElement {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun getStub(): RAssignmentStub = myStub

  override fun getElementType(): IStubElementType<out StubElement<*>, *> = stub.stubType

  override fun isLeft(): Boolean = true

  override fun isRight(): Boolean = !isLeft

  override fun isEqual(): Boolean = false

  override fun isClosureAssignment(): Boolean = false

  override fun getAssignedValue(): RExpression? = null

  override fun getAssignee(): RExpression? = null

  override fun getName(): String = myStub.name

  override fun getNameIdentifier(): PsiNamedElement? = null

  override fun isFunctionDeclaration(): Boolean = myStub.isFunctionDeclaration

  override fun getFunctionParameters(): String = if (this.isFunctionDeclaration()) "(" + myStub.parameters + ")" else ""

  private val parameterNameListValue: List<String> by lazy l@{
    if (!isFunctionDeclaration()) return@l emptyList<String>()
    return@l (RElementFactory.createRPsiElementFromText(project, "function " + functionParameters) as? RFunctionExpression)
      ?.parameterList?.parameterList?.map{ it.name } ?: emptyList<String>()
  }

  override fun getParameterNameList(): List<String> {
    return parameterNameListValue
  }

  override fun canNavigate(): Boolean {
    return super.canNavigate() && RInterpreterManager.getInstance(project).interpreter != null
  }

  override fun navigate(requestFocus: Boolean) {
    decompiled?.get()?.let {
      FileEditorManager.getInstance(project).openFile(it, requestFocus, true)
      return
    }

    val rInterpreter = RInterpreterManager.getInstance(project).interpreter ?: throw IllegalStateException("Fail")

    val promise = AsyncPromise<CharSequence>()
    runBackgroundableTask("Decompile " + name, project, true) {
      try {
        promise.setResult(RSkeletonFileDecompiler.decompileSymbol(name, containingFile.virtualFile, rInterpreter))
      }
      catch(e: Throwable) {
        promise.setError(e)
        throw e
      }
    }

    val decompilingWaiter = ThrowableComputable<CharSequence?, RuntimeException> l@{
      val indicator = ProgressIndicatorProvider.getInstance().progressIndicator
      while (true) {
        try {
          return@l promise.blockingGet(100)
        }
        catch (e: TimeoutException) {
          // do nothing
        }
        if (indicator.isCanceled) return@l null
      }
      @Suppress("UNREACHABLE_CODE") // Thank you, Kotlin Compiler!
      return@l null
    }

    val methodContent = ProgressManager.getInstance().runProcessWithProgressSynchronously(decompilingWaiter,
                                                                                          "Generate source",
                                                                                          true, project)
    if (methodContent != null) {
      val destination = ReadOnlyLightVirtualFile(name, RLanguage.INSTANCE, methodContent)
      FileEditorManager.getInstance(project).openFile(destination, true, true)
      decompiled = SoftReference(destination)
    }
  }
}