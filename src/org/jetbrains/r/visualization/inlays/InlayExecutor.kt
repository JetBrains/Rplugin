/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.visualization.inlays

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.sync.Mutex


@Service(Service.Level.APP)
internal class InlayExecutor {
  private val mutex = Mutex()

  companion object {
    fun getInstance(): InlayExecutor = service()

    val mutex: Mutex
      get() = getInstance().mutex
  }
}
