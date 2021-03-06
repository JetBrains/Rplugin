/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.r6

import com.intellij.openapi.util.io.DataInputOutputUtilRt
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef

// no need to care about overloads because R6 lib doesn't support it:
// "All items in public, private, and active must have unique names."
interface IR6ClassMember { val name: String }
data class R6ClassField(override val name: String, val isPublic: Boolean = true) : IR6ClassMember
data class R6ClassMethod(override val name: String, val parametersList: String, val isPublic: Boolean = true) : IR6ClassMember
data class R6ClassActiveBinding(override val name: String) : IR6ClassMember

data class R6ClassInfo(val className: String,
                       val superClasses: List<String>,
                       val fields: List<R6ClassField>,
                       val methods: List<R6ClassMethod>,
                       val activeBindings: List<R6ClassActiveBinding>) {

  fun containsMember(memberName: String) : Boolean {
    return ((fields + methods).map { it.name }.contains(memberName) ||
            activeBindings.map { it.name }.contains(memberName))
  }

  fun serialize(dataStream: StubOutputStream) {
    dataStream.writeName(className)
    DataInputOutputUtilRt.writeSeq(dataStream, superClasses) { dataStream.writeName(it) }

    DataInputOutputUtilRt.writeSeq(dataStream, fields) {
      dataStream.writeName(it.name)
      dataStream.writeBoolean(it.isPublic)
    }

    DataInputOutputUtilRt.writeSeq(dataStream, methods) {
      dataStream.writeName(it.name)
      dataStream.writeName(it.parametersList)
      dataStream.writeBoolean(it.isPublic)
    }

    DataInputOutputUtilRt.writeSeq(dataStream, activeBindings) {
      dataStream.writeName(it.name)
    }
  }

  companion object {
    fun deserialize(dataStream: StubInputStream): R6ClassInfo {
      val className = StringRef.toString(dataStream.readName())
      val superClasses = DataInputOutputUtilRt.readSeq(dataStream) { StringRef.toString(dataStream.readName()) }

      val fields = DataInputOutputUtilRt.readSeq(dataStream) {
        val name = StringRef.toString(dataStream.readName())
        val isPublic = dataStream.readBoolean()
        R6ClassField(name, isPublic)
      }

      val methods = DataInputOutputUtilRt.readSeq(dataStream) {
        val name = StringRef.toString(dataStream.readName())
        val parametersList = dataStream.readNameString()!!
        val isPublic = dataStream.readBoolean()
        R6ClassMethod(name, parametersList, isPublic)
      }

      val activeBindings = DataInputOutputUtilRt.readSeq(dataStream) {
        val name = StringRef.toString(dataStream.readName())
        R6ClassActiveBinding(name)
      }

      return R6ClassInfo(className, superClasses, fields, methods, activeBindings)
    }
  }
}

class R6ClassKeywordsProvider {
  companion object {
    val predefinedClassMethods = listOf(
      R6ClassMethod("clone", "", true)
    )

    val visibilityModifiers = listOf(
      R6ClassInfoUtil.argumentPrivate,
      R6ClassInfoUtil.argumentPublic,
      R6ClassInfoUtil.argumentActive,
    )
  }
}