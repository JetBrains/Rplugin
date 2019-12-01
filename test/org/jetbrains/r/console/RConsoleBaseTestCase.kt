/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.util.Disposer
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

abstract class RConsoleBaseTestCase : RProcessHandlerBaseTestCase() {
  protected lateinit var console: RConsoleView
    private set

  override fun setUp() {
    super.setUp()
    console = RConsoleView(rInterop, "dummyPath", "Test R Console")
    console.createDebuggerPanel()
    console.component // initialize editor and more...
    RConsoleManager.getInstance(project).setCurrentConsoleForTests(console)
    // console is not running command, it just haven't received the first prompt from rwrapper
    var i = 0
    while (console.isRunningCommand && i++ < 100) { Thread.sleep(20) }
    check(!console.isRunningCommand) { "Cannot get prompt from rwrapper" }
  }

  override fun tearDown() {
    Disposer.dispose(console)
    RConsoleManager.getInstance(project).setCurrentConsoleForTests(null)
    super.tearDown()
  }
}