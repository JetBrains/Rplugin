/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.interpreter

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.r.psi.util.tryRegisterDisposable
import java.nio.file.Path

abstract class RInterpreterBase(location: RInterpreterLocation, override val project: Project) : RInterpreter {
  override val interpreterLocation = location
  private val fsNotifier by lazy {
    RFsNotifier(this).also { project.tryRegisterDisposable(it) }
  }

  open fun onSetAsProjectInterpreter() {
  }

  open fun onUnsetAsProjectInterpreter() {
  }

  override fun addFsNotifierListenerForHost(roots: List<String>, parentDisposable: Disposable, listener: (Path) -> Unit) {
    fsNotifier.addListener(roots, parentDisposable, listener)
  }

  companion object {
    val LOG = Logger.getInstance(RInterpreterBase::class.java)
  }
}