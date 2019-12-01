// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.openapi.project.Project

data class RVar(val name: String, val ref: RRef, val value: RValue) {
  val project: Project
    get() = ref.rInterop.project
}

sealed class RValue
class RValueUnevaluated(val code: String): RValue()
class RValueSimple(val text: String, val isComplete: Boolean = true, val isVector: Boolean = false): RValue()
class RValueDataFrame(val rows: Int, val cols: Int): RValue()
class RValueList(val length: Int) : RValue()
class RValueFunction(val header: String): RValue()
class RValueEnvironment(val envName: String): RValue()
class RValueError(val text: String): RValue()
object RValueGraph : RValue()
