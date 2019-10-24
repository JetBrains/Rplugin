// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.intentions;

import org.jetbrains.annotations.NotNull;


/**
 * @author Holger Brandl
 */
public class TtoTrueIntentionTest extends AbstractRIntentionTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testBooleanAssignment() throws Throwable {
        doExprTest("foo = <caret>T", "foo = TRUE<caret>");
    }

    public void testLoopCheck() throws Throwable {
        doTest();
    }

    @Override
    @NotNull
    protected String getIntentionName() {
        return new TtoTrueIntention().getText();
    }
}
