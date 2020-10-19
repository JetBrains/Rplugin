/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.mock

import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.r.interpreter.RInterpreterState
import org.jetbrains.r.interpreter.RInterpreterStateManager

class MockInterpreterStateManager(project: Project) : RInterpreterStateManager {
  override val currentStateOrNull: RInterpreterState = MockInterpreterState(project, MockInterpreterStateProvider.DUMMY)
  override val states: List<RInterpreterState> = listOf(currentStateOrNull)

  override fun getCurrentStateAsync(): Promise<RInterpreterState> = resolvedPromise(currentStateOrNull)
}
