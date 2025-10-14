/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes

import com.intellij.psi.PsiElement
import com.intellij.r.psi.RLanguage
import com.intellij.r.psi.psi.api.RAssignmentStatement
import com.intellij.r.psi.psi.api.RCallExpression
import com.intellij.r.psi.psi.api.RFile
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

abstract class RClassesUtilTestsBase : RProcessHandlerBaseTestCase() {
  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  protected fun getRCallExpressionFromAssignment(assignmentStatement: RAssignmentStatement): RCallExpression? {

    if (assignmentStatement.children?.size!! < 3) return null
    return assignmentStatement.children[2] as RCallExpression
  }

  protected fun getRootElementOfPsi(code: String): PsiElement {
    val rFile = myFixture.configureByText("foo.R", code) as RFile
    val viewProvider = rFile.getViewProvider()
    val rPsi = viewProvider.getPsi(RLanguage.INSTANCE)
    return rPsi.originalElement.firstChild
  }
}