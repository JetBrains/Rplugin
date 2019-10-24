/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.openapi.util.Version

interface RInterpreterInfo {
  val interpreterName: String
  val interpreterPath: String
  val version: Version
}

data class RBasicInterpreterInfo(
  override val interpreterName: String,
  override val interpreterPath: String,
  override val version: Version
) : RInterpreterInfo
