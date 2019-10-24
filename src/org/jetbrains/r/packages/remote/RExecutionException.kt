// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote

import com.intellij.execution.ExecutionException

class RExecutionException(
  message: String,
  val command: String,
  val stdout: String,
  val stderr: String,
  val exitCode: Int
) : ExecutionException(message)
