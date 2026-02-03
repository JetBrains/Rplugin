// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.google.common.collect.Iterables
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.RFileType
import com.intellij.r.psi.psi.api.RAssignmentStatement
import com.intellij.r.psi.psi.api.RExpression
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertThat
import java.util.Objects

class UnresolvedReferenceInspectionTest : RInspectionTest() {

  override val inspection: Class<out RInspection>
    get() = UnresolvedReferenceInspection::class.java

  override fun setUp() {
    super.setUp()
    addLibraries()
  }
  // positive tests: symbols that should be resolvable. These test actually test the resolver itself and not so
  // much the inspection code

  fun testIrisReassignment() {
    doExprTest("iris = iris")
  }

  fun testNoWarningForOverriddenMethod() {
    doTest(getTestName(false) + RFileType.DOT_R_EXTENSION)
  }

  fun testOutsideBlockUsage() {
    // outside a should be resolvable
    doExprTest("{ a = 3; }; a")
  }

  fun testUsageOutsideIfElse() {
    // outside a and b should be resolvable because of r scoping rules
    doExprTest("if(TRUE)\n{ a = 3; }else{\n b = 2; }; a ; b")
  }

  fun testUnresovableSymbolInScope() {
    doTest()
  }

  fun testUnresolvableFunction() {
    doTest()
  }

  fun testPackageNameInLibraryCall() {
    doTest()
  }

  fun testForwardSelfAssignment() {
    doExprTest("sdf = { sdf }")
  }

  fun testForwardReference() {
    doExprTest("foo = { bar } ; bar = 1")
  }

  fun testFindFirstForwardReference() {
    doExprTest("foo = { bar } ; bar = 1")
  }

  fun testRedefinedReferenceLookup() {
    // no warning is expected here but do we correctly reveal the second assignment as reference for a?
    val fixture = doExprTest("a = 2; a = 3; b = a")

    val assignments = PsiTreeUtil.findChildrenOfType(fixture.file, RAssignmentStatement::class.java)
    assertSize(3, assignments)

    val value = Iterables.getLast(assignments).assignedValue
    assertNotNull(value)
    val reference = value!!.reference
    assertNotNull(reference)
    val aResolved = reference!!.resolve()

    assertNotNull(aResolved)
    assertThat<PsiElement>(aResolved, instanceOf(RAssignmentStatement::class.java))
    assertEquals(Objects.requireNonNull<RExpression>((aResolved as RAssignmentStatement).assignedValue).text, "2")
  }

  fun testUnamedCallArgumentInFunctionBody() {
    doExprTest("function() head(sdf)")
  }

  fun testNamedCallArgumentInFunctionBody() {
    doExprTest("function() head(x=sdf)")
  }

  fun testDoubleQuotedOpDef() {
    doExprTest("\"%foo%\" <- function(a,b) 3; 1 %foo% 3")
  }

  fun testBackTickOpDef() {
    doExprTest("`%foo%` <- function(a,b) 3; 1 %foo% 3")
  }

  fun testS4ClassName() {
    doWeakTest()
  }
}
