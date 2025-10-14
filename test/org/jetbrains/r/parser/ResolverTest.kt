// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.parser

import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.psi.api.RExpression
import com.intellij.r.psi.psi.api.ROperatorExpression

/**
 * @author Holger Brandl
 */
class ResolverTest : AbstractResolverTest() {

    override fun setUp() {
        super.setUp()
        addLibraries()
    }

    fun testResolveFunctionWithoutCall() {
        checkExpression("require(dplyr); glimpse")
    }

    fun testSpecialConstantsFromBase() {
        checkExpression("""
        NaN
        NA
        Inf
        """)

        // todo are there others?
    }


    fun testSimpleAssignment() {
        checkExpression("foo = 23; 16 + foo")
    }

    fun testBlockResolve() {
        checkExpression("""
        foo = 3

        {
            foo + 1
        }
        """)
    }

    fun testBlockAssignment() {
        checkExpression("""
        foo = { 1 + 1 }
        foo
        """)
    }


    fun testVarUsedInIf() {
        checkExpression("""
            x=3
            if(TRUE){
                x + 3
            }
            """)
    }

    fun testLocalFunctionCall() {
        checkExpression("foo = function(x) x; foo(3)")
    }

    fun testNsOperatorRef() {
        checkExpression("""dplyr::`%>%`""")
    }

    fun testOperatorQuoteModes() {
        // we should resolve both ops and also find their usage
        checkExpression("""
        `%foo%` <- function(a,b) 3

        1 %foo% 3

        '%bar%' <- function(a,b) 3;

        1 %bar% 3
        """)

        // do additional testing here
        val symbol = PsiTreeUtil.findChildrenOfType(myFixture.file!!, ROperatorExpression::class.java).last().operator
        assertResolvant("'%bar%' <- function(a,b) 3", symbol!!.reference)
    }


    val dotMatcher: PsiElementPattern.Capture<RExpression> = psiElement(RExpression::class.java)
            .withText(".")
            .withParent(psiElement(com.intellij.r.psi.psi.api.RArgumentList::class.java))


    fun testDontFlagLhsCallInPipe() {
        // we had a regression that the LHS was incorrectly considered as pipe-target.
        // This test should avoid it from happening again

        checkExpression("""
        library(magrittr)
        names(iris) %>% make.unique(sep= "_")
        """)
    }
}