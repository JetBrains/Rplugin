/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.openapi.util.SystemInfo

enum class OperatingSystem {
  WINDOWS, LINUX, MAC_OS;

  companion object {
    fun current(): OperatingSystem {
      return when {
        SystemInfo.isWindows -> WINDOWS
        SystemInfo.isLinux -> LINUX
        SystemInfo.isMac -> MAC_OS
        else -> throw RuntimeException("Unsupported platform")
      }
    }
  }
}