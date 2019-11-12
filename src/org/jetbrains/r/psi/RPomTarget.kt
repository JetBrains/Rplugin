/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi

import com.intellij.openapi.project.Project
import com.intellij.pom.PomTarget
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PomTargetPsiElementImpl
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.editor.RLightVirtualFileManager
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

  companion object {
    fun createPsiElementByRValue(rVar: RVar): RPsiElement = RPomTargetPsiElementImpl(
      createPomTarget(rVar), rVar.project)

    fun createSkeletonParameterTarget(assignment: RSkeletonAssignmentStatement, name: String): RPsiElement =
      RPomTargetPsiElementImpl(RSkeletonParameterPomTarget(assignment, name), assignment.project)
  }
}

private fun createPomTarget(rVar: RVar): PomTarget = when (rVar.value) {
  is RValueFunction -> createFunctionPomTarget(rVar)
  is RValueSimple -> createVariablePomTarget(rVar)
  is RValueList -> createVariablePomTarget(rVar)
  is RValueEnvironment -> createVariablePomTarget(rVar)
  is RValueDataFrame -> createDataFramePomTarget(rVar)
  else -> throw IllegalArgumentException("${rVar.javaClass} is not supported")
}

private fun createDataFramePomTarget(rVar: RVar): PomTarget = DataFramePomTarget(rVar)

private fun createVariablePomTarget(rVar: RVar): PomTarget = VariablePomTarget(rVar)

private fun createFunctionPomTarget(rVar: RVar): PomTarget = FunctionPomTarget(rVar)

internal class FunctionPomTarget(private val rVar: RVar) : RPomTarget() {
  override fun navigate(requestFocus: Boolean) {
    val rLightVirtualFileManager = RLightVirtualFileManager.getInstance(rVar.project)
    rLightVirtualFileManager.openLightFileWithContent(rVar.ref.proto.toString(), rVar.name, (rVar.value as RValueFunction).code)
  }
}

internal class VariablePomTarget(private val rVar: RVar) : RPomTarget() {
  override fun navigate(requestFocus: Boolean) {
    val console = RConsoleManager.getInstance(rVar.project).currentConsoleOrNull ?: return
    console.debugger.navigate(rVar)
  }
}

internal class DataFramePomTarget(private val rVar: RVar) : RPomTarget() {
  override fun navigate(requestFocus: Boolean) {
    VisualizeTableHandler.visualizeTable(rVar.ref.rInterop, rVar.ref, rVar.project, rVar.name)
  }
}

internal class RSkeletonParameterPomTarget(private val assignment: RSkeletonAssignmentStatement,
                                           private val name: String) : RPomTarget() {

  override fun navigate(requestFocus: Boolean) {
    RConsoleManager.getInstance(assignment.project).currentConsoleAsync.onError {
      throw IllegalStateException("Cannot get console")
    }.onSuccess { console ->
      val rVar = assignment.createRVar(console)
      val rLightVirtualFileManager = RLightVirtualFileManager.getInstance(rVar.project)
      val virtualFile = rLightVirtualFileManager.openLightFileWithContent(rVar.ref.proto.toString(), rVar.name,
                                                                          (rVar.value as RValueFunction).code)
      val psiFile = PsiManager.getInstance(assignment.project).findFile(virtualFile)
      if (psiFile !is RFile) return@onSuccess
      val rFunctionExpression = PsiTreeUtil.findChildOfAnyType(psiFile, RFunctionExpression::class.java) ?: return@onSuccess
      val parameter = rFunctionExpression.parameterList.parameterList.first { it.name == name }
      val editor = PsiUtilBase.findEditor(parameter) ?: return@onSuccess
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