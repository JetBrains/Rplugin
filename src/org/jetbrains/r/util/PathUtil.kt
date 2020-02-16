/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.util

import java.nio.file.InvalidPathException
import java.nio.file.Path

object PathUtil {
  fun toPath(first: String, vararg more: String): Path? {
    return try {
      Path.of(first, *more)
    }
    catch (e: InvalidPathException) {
      null
    }
  }
}