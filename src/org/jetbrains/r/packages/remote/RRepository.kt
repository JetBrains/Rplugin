// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote

sealed class RRepository {
  abstract val url: String
  abstract val isOptional: Boolean

  override fun toString(): String {
    return url
  }
}

data class RDefaultRepository(override val url: String, override val isOptional: Boolean) : RRepository()

data class RUserRepository(override val url: String) : RRepository() {
  override val isOptional = true
}
