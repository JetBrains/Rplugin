/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.skeleton.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.RPluginCoroutineScope
import com.intellij.r.psi.classes.s4.methods.RS4MethodsUtil.associatedS4MethodInfo
import com.intellij.r.psi.classes.s4.methods.RS4RawMethodInfo
import com.intellij.r.psi.interpreter.RInterpreterManager
import com.intellij.r.psi.packages.LibrarySummary.RLibrarySymbol.Type
import com.intellij.r.psi.packages.RSkeletonUtilPsi
import com.intellij.r.psi.psi.RElementFactory
import com.intellij.r.psi.psi.RPomTarget
import com.intellij.r.psi.psi.api.RAssignmentStatement
import com.intellij.r.psi.psi.api.RExpression
import com.intellij.r.psi.psi.api.RFunctionExpression
import com.intellij.r.psi.psi.references.RReferenceBase
import com.intellij.r.psi.refactoring.quoteIfNeeded
import com.intellij.r.psi.refactoring.rNamesValidator
import com.intellij.r.psi.rinterop.*
import com.intellij.util.IncorrectOperationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
               ?.parameterList?.parameterList?.map { it.name } ?: emptyList<String>()
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
    RPluginCoroutineScope.getScope(project).launch(Dispatchers.IO) {
      val console = RInteropManager.getInstance(project).currentConsoleInteropOrStart()
      val createPsiElementByRValue = RPomTarget.createPsiElementByRValue(createRVar(console))
      createPsiElementByRValue.navigate(requestFocus)
    }
  }

  fun createRVar(rInterop: RInterop): RVar {
    val (packageName, _) = RSkeletonUtilPsi.skeletonFileToRPackage(containingFile) ?: throw IllegalStateException("bad filename")

    val accessOperator = if (stub.exported) "::" else ":::"
    val name = rNamesValidator.quoteIfNeeded(name, rInterop.project)
    val expressionRef = RReference.expressionRef(
      if (stub.type == Type.S4METHOD) {
        val types = (associatedS4MethodInfo as RS4RawMethodInfo).parameters.joinToString(", ") { "'${it.type}'" }
        "methods::selectMethod('$name', signature = c($types))"
      }
      else {
        "$packageName$accessOperator$name"
      },
      rInterop
    )
    val rValue = try {
      expressionRef.getValueInfo()
    }
    catch (e: RInteropTerminated) {
      RValueError(RBundle.message("rinterop.terminated"))
    }
    return RVar(name, expressionRef, rValue)
  }
}