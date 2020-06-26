/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import com.intellij.openapi.Disposable
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.rinterop.RInterop

/**
 * Abstraction of (potentially remote) directory where a graphics device
 * can store its plots
 */
class RDeviceGroup(val id: String, private val interop: RInterop) : Disposable {
  override fun dispose() {
    if (interop.isAlive) {  // Note: the group is removed automatically on interop termination
      runAsync {  // Note: prevent execution on EDT
        interop.graphicsRemoveGroup(id)
      }
    }
  }
}
