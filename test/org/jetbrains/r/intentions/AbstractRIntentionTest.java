// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.intentions;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author Holger Brandl
 */
public abstract class AbstractRIntentionTest extends CodeInsightFixtureTestCase {

    public static final String TEST_DATA_PATH = new File("testData").getAbsolutePath().replace(File.pathSeparatorChar, '/');


    @Override
    public void setUp() throws Exception {
        super.setUp();
        String intentionDataPath = TEST_DATA_PATH + "/intentions/" + getClass().getSimpleName().replace("Test", "");
        myFixture.setTestDataPath(intentionDataPath);
    }

    protected void doTest() {
        myFixture.configureByFile(getTestName(false) + ".before.R");

        final IntentionAction intention = myFixture.findSingleIntention(getIntentionName());
        myFixture.launchAction(intention);
        myFixture.checkResultByFile(getTestName(false) + ".after.R");
    }


    protected void doExprTest(String before, String after) {
        myFixture.configureByText("a.R", before);

        final IntentionAction intention = myFixture.findSingleIntention(getIntentionName());
        myFixture.launchAction(intention);

        myFixture.checkResult(after);
    }


    @NotNull
    protected abstract String getIntentionName();
}
