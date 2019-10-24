// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring;

import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import org.jetbrains.r.psi.api.RAssignmentStatement;

/**
 * @author Holger Brandl
 */
public class InlineAssignmentTest extends LightPlatformCodeInsightFixtureTestCase {

    public void testTokenSimple() throws Exception {
        doTest("inline = 4\ninline + inline", "4 + 4");
    }


    public void testTokenSimple2() throws Exception {
        doTest("inline = 8\ninline + inline", "8 + 8");
    }

    // todo add more tests here


    private void doTest(/*@Language("R")*/ String text, /*@Language("R")*/ String expected) {
        PsiFile file = myFixture.configureByText("a.r", text);

        RAssignmentStatement rule = PsiTreeUtil.getChildOfType(file, RAssignmentStatement.class);
        assertNotNull(rule);

        new InlineAssignmentProcessor(rule, getProject(), null, false).run();
        assertSameLines(expected, file.getText());
    }
}
