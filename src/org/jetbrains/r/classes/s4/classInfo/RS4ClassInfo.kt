/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.s4.classInfo

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.DataInputOutputUtilRt
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.r.RFileType

data class RS4ClassSlot(val name: String, val type: String, val declarationClass: String) {
  fun serialize(dataStream: StubOutputStream) {
    dataStream.writeName(name)
    dataStream.writeName(type)
    dataStream.writeName(declarationClass)
  }

  companion object {
    fun deserialize(dataStream: StubInputStream): RS4ClassSlot {
      val name = StringRef.toString(dataStream.readName())
      val type = StringRef.toString(dataStream.readName())
      val declarationClass = StringRef.toString(dataStream.readName())
      return RS4ClassSlot(name, type, declarationClass)
    }
  }
}

data class RS4SuperClass(val name: String, val distance: Int) {

  fun serialize(dataStream: StubOutputStream) {
    dataStream.writeName(name)
    dataStream.writeInt(distance)
  }

  companion object {
    fun deserialize(dataStream: StubInputStream): RS4SuperClass {
      val name = StringRef.toString(dataStream.readName())
      val distance = dataStream.readInt()
      return RS4SuperClass(name, distance)
    }
  }
}

data class RS4ClassInfo(val className: String,
                        val packageName: String,
                        val slots: List<RS4ClassSlot>,
                        val superClasses: List<RS4SuperClass>,
                        val isVirtual: Boolean) {
  fun serialize(dataStream: StubOutputStream) {
    dataStream.writeName(className)
    dataStream.writeName(packageName)
    DataInputOutputUtilRt.writeSeq(dataStream, slots) { it.serialize(dataStream) }
    DataInputOutputUtilRt.writeSeq(dataStream, superClasses) { it.serialize(dataStream) }
    dataStream.writeBoolean(isVirtual)
  }

  fun getDeclarationText(project: Project): String {
    val indentSize = CodeStyle.getSettings(project).getIndentOptions(RFileType).INDENT_SIZE
    return buildString {
      append("$SET_CLASS_CALL_STR'").append(className).append("'")
      val containsVec = superClasses.filter { it.distance == 1 }.map { "'${it.name}'" }.toMutableList()
      if (isVirtual) containsVec.add("'VIRTUAL'")
      appendNamedVectorArg("contains", containsVec, indentSize)

      val slotsVec = slots.filter { it.declarationClass == className }.map { "${it.name} = '${it.type}'" }
      appendNamedVectorArg("slots", slotsVec, indentSize)
      if (containsVec.isNotEmpty() || slotsVec.isNotEmpty()) {
        append("\n")
      }
      append(")")
    }
  }

  private fun StringBuilder.appendNamedVectorArg(argName: String, vector: List<String>, indentSize: Int): StringBuilder {
    if (vector.isEmpty()) return this
    append(",").appendLnWithIndent("$argName = c(", COMMON_INDENT)
    if (vector.size == 1) {
      append(vector.single())
    }
    else {
      vector.forEachIndexed { ind, str ->
        if (ind != 0) append(",")
        appendLnWithIndent(str, COMMON_INDENT + indentSize)
      }
      appendLnWithIndent("", COMMON_INDENT)
    }
    append(")")
    return this
  }

  private fun StringBuilder.appendLnWithIndent(str: String, indentSize: Int) = append("\n").append(" ".repeat(indentSize)).append(str)

  companion object {

    private const val SET_CLASS_CALL_STR = "setClass("
    private const val COMMON_INDENT = SET_CLASS_CALL_STR.length

    fun deserialize(dataStream: StubInputStream): RS4ClassInfo {
      val className = StringRef.toString(dataStream.readName())
      val packageName = StringRef.toString(dataStream.readName())
      val slots = DataInputOutputUtilRt.readSeq(dataStream) { RS4ClassSlot.deserialize(dataStream) }
      val superClasses = DataInputOutputUtilRt.readSeq(dataStream) { RS4SuperClass.deserialize(dataStream) }
      val isVirtual = dataStream.readBoolean()
      return RS4ClassInfo(className, packageName, slots, superClasses, isVirtual)
    }
  }
}