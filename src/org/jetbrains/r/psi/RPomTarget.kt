/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi

import com.intellij.lang.Language
import com.intellij.openapi.application.*
import com.intellij.openapi.project.Project
import com.intellij.pom.PomNamedTarget
import com.intellij.pom.PomTarget
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PomTargetPsiElementImpl
import com.intellij.psi.util.PsiTreeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.concurrency.await
import org.jetbrains.r.RLanguage
import org.jetbrains.r.RPluginCoroutineScope
import org.jetbrains.r.classes.s4.classInfo.*
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.debugger.RDebuggerUtil
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.rinterop.*
import org.jetbrains.r.run.visualize.VisualizeTableHandler
import org.jetbrains.r.skeleton.psi.RSkeletonAssignmentStatement
import org.jetbrains.r.skeleton.psi.RSkeletonCallExpression

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

internal class FunctionPomTarget(private val rVar: RVar) : RPomTarget() {
  override suspend fun navigateAsync(requestFocus: Boolean) {
    return rVar.ref.rInterop.executeTask {
      rVar.ref.functionSourcePositionWithText()?.let {
        ApplicationManager.getApplication().invokeLater {
          RDebuggerUtil.navigateAndCheckSourceChanges(rVar.project, it)
        }
      }
      Unit
    }.await()
  }

  override fun getName(): String = rVar.name
}

internal class VariablePomTarget(private val rVar: RVar) : RPomTarget() {
  override suspend fun navigateAsync(requestFocus: Boolean) {
    withContext(Dispatchers.EDT) {
      RConsoleManager.getInstance(rVar.project).currentConsoleOrNull?.debuggerPanel?.navigate(rVar)
    }
  }

  override fun getName(): String = rVar.name
}

internal class DataFramePomTarget(private val rVar: RVar) : RPomTarget() {
  override suspend fun navigateAsync(requestFocus: Boolean) {
    VisualizeTableHandler.visualizeTable(rVar.ref.rInterop, rVar.ref, rVar.project, rVar.name)
  }

  override fun getName(): String = rVar.name
}

internal class GraphPomTarget(private val rVar: RVar) : RPomTarget() {
  override suspend fun navigateAsync(requestFocus: Boolean) {
    rVar.ref.evaluateAsTextAsync().await()
  }

  override fun getName(): String = rVar.name
}

internal class RSkeletonParameterPomTarget(val assignment: RSkeletonAssignmentStatement,
                                           val parameterName: String) : RPomTarget() {

  override suspend fun navigateAsync(requestFocus: Boolean) {
    val console = RConsoleManager.getInstance(assignment.project).awaitCurrentConsole().getOrThrow()

    withContext(Dispatchers.IO) {
      val rVar = assignment.createRVar(console)
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