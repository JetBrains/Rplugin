/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.r.blockingGetAndDispatchEvents
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

abstract class RConsoleBaseTestCase : RProcessHandlerBaseTestCase() {
  protected lateinit var console: RConsoleView
    private set

  override fun setUp() {
    super.setUp()
    val rConsoleRunner = RConsoleRunner(interpreter)
    val promise = rConsoleRunner.initByInterop(rInterop)

    console = promise.blockingGetAndDispatchEvents(DEFAULT_TIMEOUT) ?: error("Cannot initialize test console")
    console.component // initialize editor and more...
    RConsoleManager.getInstance(project).setCurrentConsoleForTests(console)
    // console is not running command, it just haven't received the first prompt from rwrapper
    var i = 0
    while (console.isRunningCommand && i++ < 100) { Thread.sleep(20) }
    check(!console.isRunningCommand) { "Cannot get prompt from rwrapper" }
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
  }

  override fun tearDown() {
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    Disposer.dispose(console)
    RConsoleManager.getInstance(project).setCurrentConsoleForTests(null)
    super.tearDown()
  }
}