// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.rinterop

import com.intellij.openapi.project.Project

data class RVar(val name: String, val ref: RReference, val value: RValue) {
  val project: Project
    get() = ref.rInterop.project
}
