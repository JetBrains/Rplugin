/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.mock

import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterLocation
import org.jetbrains.r.interpreter.RInterpreterManager

class MockInterpreterManager(project: Project) : RInterpreterManager {
  override val isSkeletonInitialized: Boolean = true

  override val interpreterOrNull: RInterpreter = MockInterpreter(project, MockInterpreterProvider.DUMMY)

  override val interpreterLocation: RInterpreterLocation
    get() = interpreterOrNull.interpreterLocation

  override fun getInterpreterAsync(force: Boolean) = resolvedPromise(interpreterOrNull)

  override fun hasInterpreter(): Boolean = true
}
