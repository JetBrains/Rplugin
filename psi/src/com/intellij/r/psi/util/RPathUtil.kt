/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.util

import java.nio.file.InvalidPathException
import java.nio.file.Path

object RPathUtil {
  fun toPath(first: String, vararg more: String): Path? {
    return try {
      Path.of(first, *more)
    }
    catch (e: InvalidPathException) {
      null
    }
  }

  fun join(first: String, vararg more: String): String {
    fun isSlash(c: Char) = c == '/' || c == '\\'

    val result = StringBuilder(first.dropLastWhile(::isSlash))
    for (s in more) {
      result.append('/').append(s.trim(::isSlash))
    }
    return result.toString().replace('\\', '/')
  }
}
