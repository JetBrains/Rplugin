/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RInteropUtil

abstract class RProcessHandlerBaseTestCase : RUsefulTestCase() {
  protected lateinit var rInterop: RInterop

  override fun setUp() {
    super.setUp()
    rInterop = getRInterop(project)
    // we want be sure that the interpreter is initialized
    rInterop.executeCode("1")
  }

  override fun tearDown() {
    if (!Disposer.isDisposed(rInterop)) {
      Disposer.dispose(rInterop)
    }
    super.tearDown()
  }

  companion object {
    const val DEFAULT_TIMEOUT = 3000

    private fun getRInterop(project: Project): RInterop {
      return RInteropUtil.runRWrapperAndInterop(project).blockingGet(DEFAULT_TIMEOUT)!!
    }
  }
}
