/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.mock

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.r.psi.interpreter.RInterpreter
import com.intellij.r.psi.interpreter.RInterpreterLocation
import com.intellij.r.psi.interpreter.RInterpreterManager
import com.intellij.r.psi.interpreter.RInterpreterUtil
import com.intellij.r.psi.interpreter.RLocalInterpreterLocation
import com.intellij.r.psi.settings.RInterpreterSettingsProvider
import com.intellij.testFramework.common.timeoutRunBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Runnable

class MockInterpreterManager(project: Project) : RInterpreterManager {
  override var interpreterLocation: RInterpreterLocation? =
    RInterpreterSettingsProvider.getProviders().asSequence().mapNotNull { it.provideInterpreterForTests() }.firstOrNull() ?: RLocalInterpreterLocation(timeoutRunBlocking { RInterpreterUtil.suggestHomePath() })

  override val interpreterOrNull: RInterpreter = interpreterLocation!!.createInterpreter(project).getOrThrow()

  override fun getInterpreterDeferred(force: Boolean) = CompletableDeferred(Result.success(interpreterOrNull))

  override fun hasInterpreterLocation(): Boolean = true

  override fun restartInterpreter(afterRestart: Runnable?) {}

  init {
    LOG.warn("List of settings providers: ${RInterpreterSettingsProvider.getProviders().toList()}")
    LOG.warn("Selected interpreter location: ${interpreterLocation}")
  }

  companion object {
    val LOG = Logger.getInstance(MockInterpreterManager::class.java)
  }
}
