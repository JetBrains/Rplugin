/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run

import junit.framework.TestCase
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.interpreter.RInterpreterUtil

class InterpreterValidityTest : RUsefulTestCase() {
  fun testValidity() {
    RInterpreterUtil.suggestAllHomePaths().forEach { interpreterPath ->
      TestCase.assertNotNull(RInterpreterUtil.getVersionByPath(interpreterPath))
    }
  }
}