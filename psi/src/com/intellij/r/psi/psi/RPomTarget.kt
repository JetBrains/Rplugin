/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.pom.PomNamedTarget
import com.intellij.pom.PomTarget
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PomTargetPsiElementImpl
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.RLanguage
import com.intellij.r.psi.RPluginCoroutineScope
import com.intellij.r.psi.classes.s4.classInfo.RS4ClassSlot
import com.intellij.r.psi.classes.s4.classInfo.RS4ComplexSlotPomTarget
import com.intellij.r.psi.classes.s4.classInfo.RSkeletonS4ClassPomTarget
import com.intellij.r.psi.classes.s4.classInfo.RSkeletonS4SlotPomTarget
import com.intellij.r.psi.classes.s4.classInfo.RStringLiteralPomTarget
import com.intellij.r.psi.debugger.RDebuggerPanelManager
import com.intellij.r.psi.debugger.RDebuggerUtilPsi
import com.intellij.r.psi.psi.api.RExpression
import com.intellij.r.psi.psi.api.RFile
import com.intellij.r.psi.psi.api.RFunctionExpression
import com.intellij.r.psi.psi.api.RParameter
import com.intellij.r.psi.psi.api.RPsiElement
import com.intellij.r.psi.psi.api.RStringLiteralExpression
import com.intellij.r.psi.rinterop.RInteropManager
import com.intellij.r.psi.rinterop.RValueDataFrame
import com.intellij.r.psi.rinterop.RValueError
import com.intellij.r.psi.rinterop.RValueFunction
import com.intellij.r.psi.rinterop.RValueGraph
import com.intellij.r.psi.rinterop.RValueMatrix
import com.intellij.r.psi.rinterop.RVar
import com.intellij.r.psi.run.visualize.RVisualization
import com.intellij.r.psi.skeleton.psi.RSkeletonAssignmentStatement
import com.intellij.r.psi.skeleton.psi.RSkeletonCallExpression
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.concurrency.await

private open class RPomTargetPsiElementImpl(pomTarget: PomTarget, project: Project): PomTargetPsiElementImpl(project, pomTarget), RPsiElement {
  override fun getLanguage(): Language = RLanguage.INSTANCE
}

private class RStringLiteralPomTargetPsiElementImpl(literal: RStringLiteralExpression) :
  RPomTargetPsiElementImpl(RStringLiteralPomTarget(literal), literal.project) {
  override fun getNavigationElement() = this // hack for use RStringLiteralManipulator#getRangeInElement as target TextRange
}

abstract class RPomTarget: PomNamedTarget {
  override fun canNavigate(): Boolean = true

  override fun canNavigateToSource(): Boolean = true

  override fun isValid(): Boolean = true

  abstract suspend fun navigateAsync(requestFocus: Boolean)

  override fun navigate(requestFocus: Boolean) {
    RPluginCoroutineScope.getApplicationScope().launch(ModalityState.defaultModalityState().asContextElement()) {
      navigateAsync(requestFocus)
    }
  }

  companion object {
    fun createPsiElementByRValue(rVar: RVar): RPsiElement = RPomTargetPsiElementImpl(
      createPomTarget(rVar), rVar.project)

    fun createSkeletonParameterTarget(assignment: RSkeletonAssignmentStatement, name: String): RPsiElement =
      RPomTargetPsiElementImpl(RSkeletonParameterPomTarget(assignment, name), assignment.project)

    fun createSkeletonS4SlotTarget(setClass: RSkeletonCallExpression, name: String): RPsiElement =
      RPomTargetPsiElementImpl(RSkeletonS4SlotPomTarget(setClass, name), setClass.project)

    fun createS4ComplexSlotTarget(slotDefinition: RExpression, slot: RS4ClassSlot): RPsiElement =
      RPomTargetPsiElementImpl(RS4ComplexSlotPomTarget(slotDefinition, slot), slotDefinition.project)

    fun createSkeletonS4ClassTarget(setClass: RSkeletonCallExpression): RPsiElement =
      RPomTargetPsiElementImpl(RSkeletonS4ClassPomTarget(setClass), setClass.project)

    fun createStringLiteralTarget(literal: RStringLiteralExpression): RPsiElement =
      RStringLiteralPomTargetPsiElementImpl(literal)

    fun createPomTarget(rVar: RVar): RPomTarget = when (val value = rVar.value) {
      is RValueFunction -> FunctionPomTarget(rVar)
      is RValueDataFrame -> DataFramePomTarget(rVar)
      is RValueGraph -> GraphPomTarget(rVar)
      is RValueMatrix -> {
        if (value.dim.size == 2) DataFramePomTarget(rVar)
        else VariablePomTarget(rVar)
      }
      is RValueError -> throw IllegalStateException("Error: ${value.text}")
      else -> VariablePomTarget(rVar)
    }

    fun isSkeletonPomTargetPsi(element: PsiElement): Boolean {
      if (element !is RPomTargetPsiElementImpl) return false
      val target = element.target
      return target is RSkeletonParameterPomTarget || target is RSkeletonS4ClassPomTarget || target is RSkeletonS4SlotPomTarget
    }
  }
}

class FunctionPomTarget(private val rVar: RVar) : RPomTarget() {
  override suspend fun navigateAsync(requestFocus: Boolean) {
    return rVar.ref.rInterop.executeTask {
      rVar.ref.functionSourcePositionWithText()?.let {
        ApplicationManager.getApplication().invokeLater {
          RDebuggerUtilPsi.navigateAndCheckSourceChanges(rVar.project, it)
        }
      }
      Unit
    }.await()
  }

  override fun getName(): String = rVar.name
}

class VariablePomTarget(private val rVar: RVar) : RPomTarget() {
  override suspend fun navigateAsync(requestFocus: Boolean) {
    withContext(Dispatchers.EDT) {
      RDebuggerPanelManager.getInstance(rVar.project).navigate(rVar)
    }
  }

  override fun getName(): String = rVar.name
}

class DataFramePomTarget(private val rVar: RVar) : RPomTarget() {
  override suspend fun navigateAsync(requestFocus: Boolean) {
    RVisualization.getInstance(rVar.project).visualizeTable(rVar.ref.rInterop, rVar.ref, rVar.name)
  }

  override fun getName(): String = rVar.name
}

internal class GraphPomTarget(private val rVar: RVar) : RPomTarget() {
  override suspend fun navigateAsync(requestFocus: Boolean) {
    rVar.ref.evaluateAsTextAsync().await()
  }

  override fun getName(): String = rVar.name
}

class RSkeletonParameterPomTarget(val assignment: RSkeletonAssignmentStatement,
                                           val parameterName: String) : RPomTarget() {

  override suspend fun navigateAsync(requestFocus: Boolean) {
    val rInterop = RInteropManager.getInstance(assignment.project).currentConsoleInteropOrStart()

    withContext(Dispatchers.IO) {
      val rVar = assignment.createRVar(rInterop)
      val virtualFile = rVar.ref.functionSourcePosition()?.file ?: return@withContext

      val parameter: RParameter? = readAction {
        val psiFile = PsiManager.getInstance(assignment.project).findFile(virtualFile)
        if (psiFile !is RFile) return@readAction null
        val rFunctionExpression = PsiTreeUtil.findChildOfAnyType(psiFile, RFunctionExpression::class.java) ?: return@readAction null
        val parameter = rFunctionExpression.parameterList?.parameterList?.firstOrNull { it.name == parameterName } ?: return@readAction null
        return@readAction parameter
      }

      if (parameter != null) {
        withContext(Dispatchers.EDT) {
          parameter.navigate(true)
        }
      }
    }
  }

  override fun getName(): String = parameterName

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as RSkeletonParameterPomTarget
    if (assignment != other.assignment) return false
    if (parameterName != other.parameterName) return false
    return true
  }

  override fun hashCode(): Int {
    var result = assignment.hashCode()
    result = 31 * result + parameterName.hashCode()
    return result
  }
}