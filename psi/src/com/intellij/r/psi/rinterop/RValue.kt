package com.intellij.r.psi.rinterop

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
