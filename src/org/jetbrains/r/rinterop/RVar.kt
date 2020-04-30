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

sealed class RValue(val cls: List<String>)
class RValueUnevaluated(cls: List<String>, val code: String): RValue(cls)
class RValueSimple(cls: List<String>, val text: String, val isComplete: Boolean = true,
                   val isVector: Boolean = false, val isS4: Boolean = false): RValue(cls)
class RValueDataFrame(cls: List<String>, val rows: Int, val cols: Int): RValue(cls)
class RValueList(cls: List<String>, val length: Long) : RValue(cls)
class RValueFunction(cls: List<String>, val header: String): RValue(cls)
class RValueEnvironment(cls: List<String>, val envName: String): RValue(cls)
class RValueError(val text: String): RValue(emptyList())
class RValueGraph(cls: List<String>) : RValue(cls)
class RValueMatrix(cls: List<String>, val dim: List<Int>) : RValue(cls)
