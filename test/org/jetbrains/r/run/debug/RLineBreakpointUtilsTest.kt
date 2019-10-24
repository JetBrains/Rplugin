// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug

import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.PlatformTestCase
import junit.framework.TestCase

class RLineBreakpointUtilsTest : PlatformTestCase() {
  fun testNotRFile() {
    val file = HeavyPlatformTestCase.getVirtualFile(createTempFile("script.s", "print(\"ok\")"))!!
    TestCase.assertFalse(RLineBreakpointUtils.canPutAt(project, file, 0))
  }

  fun testWhitespaces() {
    val file = HeavyPlatformTestCase.getVirtualFile(createTempFile("script.r", "   "))!!
    TestCase.assertFalse(RLineBreakpointUtils.canPutAt(project, file, 0))
  }

  fun testComment() {
    val file = HeavyPlatformTestCase.getVirtualFile(createTempFile("script.r", "# comment"))!!
    TestCase.assertFalse(RLineBreakpointUtils.canPutAt(project, file, 0))
  }

  fun testLeftBrace() {
    val file = HeavyPlatformTestCase.getVirtualFile(createTempFile("script.r", "{"))!!
    TestCase.assertFalse(RLineBreakpointUtils.canPutAt(project, file, 0))
  }

  fun testRightBrace() {
    val file = HeavyPlatformTestCase.getVirtualFile(createTempFile("script.r", "}"))!!
    TestCase.assertFalse(RLineBreakpointUtils.canPutAt(project, file, 0))
  }

  fun testOk() {
    val file = HeavyPlatformTestCase.getVirtualFile(createTempFile("script.r", "print(\"ok\")"))!!
    TestCase.assertTrue(RLineBreakpointUtils.canPutAt(project, file, 0))
  }
}