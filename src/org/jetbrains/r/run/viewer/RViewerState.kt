/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.viewer

import java.io.File

interface RViewerState {
  val url: String?
  val tracedFile: File
  fun update()
  fun reset()
  fun addListener(listener: Listener)
  fun removeListener(listener: Listener)

  interface Listener {
    fun onCurrentChange(newUrl: String)
    fun onReset()
  }
}