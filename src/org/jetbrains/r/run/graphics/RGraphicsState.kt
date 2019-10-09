// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import java.io.File

interface LiveScreenParameters {
  val currentValue: RGraphicsUtils.ScreenParameters?
  fun addListener(listener: (RGraphicsUtils.ScreenParameters) -> Unit)
}

interface RGraphicsState {
  val snapshots: List<RSnapshot>
  val tracedDirectory: File
  val screenParameters: LiveScreenParameters
  var currentSnapshotNumber: Int?
  fun update()
  fun reset()
  fun clearSnapshot(number: Int)
  fun addListener(listener: Listener)
  fun removeListener(listener: Listener)
  fun changeScreenParameters(parameters: RGraphicsUtils.ScreenParameters)

  interface Listener {
    fun onCurrentChange(snapshots: List<RSnapshot>)
    fun onReset()
  }
}