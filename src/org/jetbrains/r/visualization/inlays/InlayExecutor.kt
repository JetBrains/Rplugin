/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.visualization.inlays

import kotlinx.coroutines.sync.Mutex


object InlayExecutor {
  val mutex = Mutex()
}
