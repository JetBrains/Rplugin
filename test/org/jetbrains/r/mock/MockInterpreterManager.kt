/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.mock

import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.r.interpreter.*
import org.jetbrains.r.settings.RInterpreterSettingsProvider

class MockInterpreterManager(project: Project) : RInterpreterManager {
  override val interpreterLocation: RInterpreterLocation =
    RInterpreterSettingsProvider.getProviders().asSequence().mapNotNull { it.provideInterpreterForTests() }.firstOrNull() ?:
      RLocalInterpreterLocation(RInterpreterUtil.suggestHomePath())

  override val interpreterOrNull: RInterpreter = interpreterLocation.createInterpreter(project)

  override fun getInterpreterAsync(force: Boolean) = resolvedPromise(interpreterOrNull)

  override fun hasInterpreter(): Boolean = true
}
