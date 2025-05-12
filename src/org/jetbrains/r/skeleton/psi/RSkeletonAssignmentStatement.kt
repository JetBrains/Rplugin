/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.skeleton.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.r.RBundle
import org.jetbrains.r.classes.s4.methods.RS4MethodsUtil.associatedS4MethodInfo
import org.jetbrains.r.classes.s4.methods.RS4RawMethodInfo
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.packages.LibrarySummary.RLibrarySymbol.Type
import org.jetbrains.r.packages.RSkeletonUtil
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.RPomTarget
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RExpression
import org.jetbrains.r.psi.api.RFunctionExpression
import org.jetbrains.r.psi.references.RReferenceBase
import org.jetbrains.r.refactoring.quoteIfNeeded
import org.jetbrains.r.refactoring.rNamesValidator
import org.jetbrains.r.rinterop.RInteropTerminated
import org.jetbrains.r.rinterop.RReference
import org.jetbrains.r.rinterop.RValueError
import org.jetbrains.r.rinterop.RVar

class RSkeletonAssignmentStatement(private val myStub: RSkeletonAssignmentStub) : RSkeletonBase(), RAssignmentStatement {
  override fun getMirror() = null

  override fun getParent(): PsiElement = myStub.getParentStub().getPsi()

  override fun setName(name: String): PsiElement {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun getStub(): RSkeletonAssignmentStub = myStub

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

  override fun isPrimitiveFunctionDeclaration(): Boolean = myStub.isPrimitiveFunctionDeclaration

  override fun getFunctionParameters(): String = if (this.isFunctionDeclaration()) "(" + myStub.parameters + ")" else ""

  private val parameterNameListValue: List<String> by lazy l@{
    if (!isFunctionDeclaration()) return@l emptyList<String>()
    return@l (RElementFactory.createRPsiElementFromText(project, "function $functionParameters") as? RFunctionExpression)
      ?.parameterList?.parameterList?.map{ it.name } ?: emptyList<String>()
  }

  override fun getParameterNameList(): List<String> {
    return parameterNameListValue
  }

  override fun canNavigate(): Boolean {
    return super<RSkeletonBase>.canNavigate() && RInterpreterManager.getInstance(project).hasInterpreterLocation()
  }

  override fun getText(): String {
    return name + functionParameters
  }

  override fun getReference(): RReferenceBase<*>? {
    return null
  }

  override fun navigate(requestFocus: Boolean) {
    RConsoleManager.getInstance(project).runAsync { console ->
      val createPsiElementByRValue = RPomTarget.createPsiElementByRValue(createRVar(console))
      createPsiElementByRValue.navigate(requestFocus)
    }
  }

  internal fun createRVar(consoleView: RConsoleView): RVar {
    val (packageName, _) = RSkeletonUtil.skeletonFileToRPackage(containingFile) ?: throw IllegalStateException("bad filename")

    val accessOperator = if (stub.exported) "::" else ":::"
    val name = rNamesValidator.quoteIfNeeded(name, consoleView.project)
    val expressionRef = RReference.expressionRef(
      if (stub.type == Type.S4METHOD) {
        val types = (associatedS4MethodInfo as RS4RawMethodInfo).parameters.joinToString(", ") { "'${it.type}'" }
        "methods::selectMethod('$name', signature = c($types))"
      }
      else {
        "$packageName$accessOperator$name"
      },
      consoleView.rInterop
    )
    val rValue = try {
      expressionRef.getValueInfo()
    } catch (e: RInteropTerminated) {
      RValueError(RBundle.message("rinterop.terminated"))
    }
    return RVar(name, expressionRef, rValue)
  }
}