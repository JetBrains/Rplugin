/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi

import com.intellij.openapi.project.Project
import com.intellij.pom.PomTarget
import com.intellij.psi.impl.PomTargetPsiElementImpl
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.editor.REditorUtil
import org.jetbrains.r.psi.api.RPsiElement
import org.jetbrains.r.rinterop.*
import org.jetbrains.r.run.visualize.VisualizeTableHandler

private class RPomTargetPsiElementImpl(pomTarget: PomTarget, project: Project): PomTargetPsiElementImpl(project, pomTarget), RPsiElement

abstract class RPomTarget: PomTarget {
  override fun canNavigate(): Boolean = true

  override fun canNavigateToSource(): Boolean = true

  override fun isValid(): Boolean = true

  companion object {
    fun createPsiElementByRValue(rVar: RVar): RPsiElement = RPomTargetPsiElementImpl(
      createPomTarget(rVar), rVar.project)
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

internal class FunctionPomTarget(private val rVar: RVar): RPomTarget() {
  override fun navigate(requestFocus: Boolean) {
    REditorUtil.createReadOnlyLightRFileAndOpen(rVar.project, rVar.name, (rVar.value as RValueFunction).code)
  }
}

internal class VariablePomTarget(private val rVar: RVar): RPomTarget() {
  override fun navigate(requestFocus: Boolean) {
    val console = RConsoleManager.getInstance(rVar.project).currentConsoleOrNull ?: return
    console.debugger.navigate(rVar)
  }
}

internal class DataFramePomTarget(private val rVar: RVar): RPomTarget() {
  override fun navigate(requestFocus: Boolean) {
    VisualizeTableHandler.visualizeTable(rVar.ref.rInterop, rVar.ref, rVar.project, rVar.name)
  }
}