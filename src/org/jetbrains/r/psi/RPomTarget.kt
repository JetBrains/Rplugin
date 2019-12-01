/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.pom.PomTarget
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PomTargetPsiElementImpl
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.psi.api.RFile
import org.jetbrains.r.psi.api.RFunctionExpression
import org.jetbrains.r.psi.api.RPsiElement
import org.jetbrains.r.rinterop.*
import org.jetbrains.r.run.visualize.VisualizeTableHandler
import org.jetbrains.r.skeleton.psi.RSkeletonAssignmentStatement

private class RPomTargetPsiElementImpl(pomTarget: PomTarget, project: Project): PomTargetPsiElementImpl(project, pomTarget), RPsiElement

abstract class RPomTarget: PomTarget {
  override fun canNavigate(): Boolean = true

  override fun canNavigateToSource(): Boolean = true

  override fun isValid(): Boolean = true

  abstract fun navigateAsync(requestFocus: Boolean): Promise<Unit>

  override fun navigate(requestFocus: Boolean) {
    navigateAsync(requestFocus)
  }

  companion object {
    fun createPsiElementByRValue(rVar: RVar): RPsiElement = RPomTargetPsiElementImpl(
      createPomTarget(rVar), rVar.project)

    fun createSkeletonParameterTarget(assignment: RSkeletonAssignmentStatement, name: String): RPsiElement =
      RPomTargetPsiElementImpl(RSkeletonParameterPomTarget(assignment, name), assignment.project)

    fun createPomTarget(rVar: RVar): RPomTarget = when (val value = rVar.value) {
      is RValueFunction -> createFunctionPomTarget(rVar)
      is RValueSimple -> createVariablePomTarget(rVar)
      is RValueList -> createVariablePomTarget(rVar)
      is RValueEnvironment -> createVariablePomTarget(rVar)
      is RValueDataFrame -> createDataFramePomTarget(rVar)
      is RValueError -> throw IllegalStateException("Error: ${value.text}")
      else -> throw IllegalArgumentException("${rVar.value.javaClass} is not supported")
    }

  }
}

private fun createDataFramePomTarget(rVar: RVar): RPomTarget = DataFramePomTarget(rVar)

private fun createVariablePomTarget(rVar: RVar): RPomTarget = VariablePomTarget(rVar)

private fun createFunctionPomTarget(rVar: RVar): RPomTarget = FunctionPomTarget(rVar)

internal class FunctionPomTarget(private val rVar: RVar) : RPomTarget() {
  override fun navigateAsync(requestFocus: Boolean): Promise<Unit> {
    return rVar.ref.rInterop.executeTask {
      rVar.ref.functionSourcePosition()?.xSourcePosition?.let {
        ApplicationManager.getApplication().invokeLater {
          it.createNavigatable(rVar.project).navigate(true)
        }
      }
      Unit
    }
  }
}

internal class VariablePomTarget(private val rVar: RVar) : RPomTarget() {
  override fun navigateAsync(requestFocus: Boolean): Promise<Unit> {
    val promise = AsyncPromise<Unit>()
    ApplicationManager.getApplication().invokeLater {
      RConsoleManager.getInstance(rVar.project).currentConsoleOrNull?.debuggerPanel?.navigate(rVar)
      promise.setResult(Unit)
    }
    return promise
  }
}

internal class DataFramePomTarget(private val rVar: RVar) : RPomTarget() {
  override fun navigateAsync(requestFocus: Boolean): Promise<Unit> {
    return VisualizeTableHandler.visualizeTable(rVar.ref.rInterop, rVar.ref, rVar.project, rVar.name)
  }
}

internal class RSkeletonParameterPomTarget(private val assignment: RSkeletonAssignmentStatement,
                                           private val name: String) : RPomTarget() {

  override fun navigateAsync(requestFocus: Boolean): Promise<Unit> {
    return RConsoleManager.getInstance(assignment.project).runAsync { console ->
      val rVar = assignment.createRVar(console)
      val virtualFile = rVar.ref.functionSourcePosition()?.file ?: return@runAsync
      val psiFile = PsiManager.getInstance(assignment.project).findFile(virtualFile)
      if (psiFile !is RFile) return@runAsync
      val rFunctionExpression = PsiTreeUtil.findChildOfAnyType(psiFile, RFunctionExpression::class.java) ?: return@runAsync
      val parameter = rFunctionExpression.parameterList.parameterList.first { it.name == name }
      val editor = PsiUtilBase.findEditor(parameter) ?: return@runAsync
      editor.caretModel.moveToOffset(parameter.textRange.startOffset)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as RSkeletonParameterPomTarget
    if (assignment != other.assignment) return false
    if (name != other.name) return false
    return true
  }

  override fun hashCode(): Int {
    var result = assignment.hashCode()
    result = 31 * result + name.hashCode()
    return result
  }
}