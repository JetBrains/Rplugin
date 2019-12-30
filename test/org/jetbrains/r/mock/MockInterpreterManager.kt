/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.mock

import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterManager

class MockInterpreterManager(project: Project) : RInterpreterManager {
  override fun initializeInterpreter(force: Boolean): Promise<Unit> {
    return AsyncPromise<Unit>().apply {
      setResult(Unit)
    }
  }

  override val interpreterPath: String
    get() = interpreter?.interpreterPath ?: ""

  override val interpreter: RInterpreter? = MockInterpreter(project, MockInterpreterProvider.DUMMY)

  override fun hasInterpreter(): Boolean = true
}
