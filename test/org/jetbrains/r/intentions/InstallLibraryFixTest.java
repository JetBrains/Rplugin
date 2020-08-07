// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.intentions;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.execution.ExecutionException;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import org.jetbrains.r.inspections.MissingPackageInspection;
import org.jetbrains.r.interpreter.RInterpreter;
import org.jetbrains.r.interpreter.RInterpreterManager;
import org.jetbrains.r.interpreter.RInterpreterUtil;
import org.jetbrains.r.packages.RInstalledPackage;
import org.jetbrains.r.packages.remote.RepoUtils;

import java.util.Collections;
import java.util.List;

/**
 * @author Holger Brandl
 */
public class InstallLibraryFixTest extends LightPlatformCodeInsightFixtureTestCase {

    // see https://intellij-support.jetbrains.com/hc/en-us/community/posts/203365330-SetupJDKFix-fails-in-LightCodeInsightFixtureTestCase
    // alsp see org.jetbrains.plugins.groovy.intentions.GroovyConvertJUnitIntentionTest


    public void testDummy() {
    }

    // TODO enable once UI disposal issue has been resolved
    public void _testPackageInstallationAction() throws Exception {
        // remove the package so that we can detect it as missing
        final String TEST_PACKAGE = "pals";

        try {
            RepoUtils.INSTANCE.uninstallPackage(null, myFixture.getProject(), new RInstalledPackage(TEST_PACKAGE, "1.0.0", null,
                                                                                                    "", "",
                                                                                                    Collections.emptyMap()));
        } catch (ExecutionException e) {
            e.printStackTrace();
            fail("could not uninstall package");
        }

        myFixture.enableInspections(MissingPackageInspection.class);
        PsiFile psiFile = myFixture.configureByText("a.R", "require(pals)");

        // not needed here
        // myFixture.testHighlighting(false, false, false);

        List<IntentionAction> quickFixes = myFixture.getAllQuickFixes();

        myFixture.launchAction(assertOneElement(quickFixes));
        Thread.sleep(30000); // what a mess!!

        RInterpreter interpreter = RInterpreterManager.Companion.getInterpreterAsync(myFixture.getProject()).blockingGet(Integer.MAX_VALUE);
        assertNotNull(interpreter);
        String commandResult =
          RInterpreterUtil.INSTANCE.runScript("require(pals) == TRUE", interpreter.getInterpreterLocation()).getStdout();
        assertNotNull(commandResult);
        assertTrue(commandResult.contains("TRUE"));
    }
}
