// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.parser

import com.intellij.psi.PsiReference
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.intellij.lang.annotations.Language
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.inspections.UnresolvedReferenceInspection
import org.jetbrains.r.inspections.UnusedVariableInspection

/**
 * @author Holger Brandl
 */
abstract class AbstractResolverTest : RUsefulTestCase() {
    protected fun assertResolvant(expected: String, reference: PsiReference) {
        val operatorResolvant = reference.resolve()

        assertEquals(expected, operatorResolvant!!.text)
    }

    protected fun checkExpression(@Language("R") expressionList: String, testWarnings: Boolean = true): CodeInsightTestFixture {
        myFixture.configureByText("a.R", expressionList)

        myFixture.enableInspections(
                UnusedVariableInspection::class.java,
                UnresolvedReferenceInspection::class.java
        );

        myFixture.testHighlighting(testWarnings, false, false)

        return myFixture
    }
}