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
data class R6ClassField(val name: String, val isPublic: Boolean = true)
data class R6ClassMethod(val name: String, val isPublic: Boolean = true)
data class R6ClassActiveBinding(val name: String)

// PUBLIC PRIVATE !!!
data class R6ClassInfo(val className: String,
                       val packageName: String,
                       val superClass: String,
                       val fields: List<R6ClassField>,
                       val methods: List<R6ClassMethod>,
                       val activeBindings: List<R6ClassActiveBinding>) {

  fun serialize(dataStream: StubOutputStream) {
    dataStream.writeName(className)
    dataStream.writeName(packageName)
    dataStream.writeName(superClass)

    DataInputOutputUtilRt.writeSeq(dataStream, fields) {
      dataStream.writeName(it.name);
      dataStream.writeBoolean(it.isPublic)
    }

    DataInputOutputUtilRt.writeSeq(dataStream, methods) {
      dataStream.writeName(it.name)
      dataStream.writeBoolean(it.isPublic);
    }

    DataInputOutputUtilRt.writeSeq(dataStream, activeBindings) {
      dataStream.writeName(it.name)
    }
  }

  companion object {
    fun createDummyFromCoupleParameters(className: String, packageName: String = "test_package"): R6ClassInfo {
      return R6ClassInfo(className = className,
                         packageName = packageName,
                         superClass = "",
                         fields = emptyList(),
                         methods = emptyList(),
                         activeBindings = emptyList())
    }

    fun empty(): R6ClassInfo {
      return R6ClassInfo(className = "",
                         packageName = "",
                         superClass = "",
                         fields = emptyList(),
                         methods = emptyList(),
                         activeBindings = emptyList())
    }

    fun deserialize(dataStream: StubInputStream): R6ClassInfo {
      val className = StringRef.toString(dataStream.readName())
      val packageName = StringRef.toString(dataStream.readName())
      val superClass = StringRef.toString(dataStream.readName())

      val fields = DataInputOutputUtilRt.readSeq(dataStream) {
        val name = StringRef.toString(dataStream.readName())
        val isPublic = dataStream.readBoolean()
        R6ClassField(name, isPublic = isPublic)
      }

      val methods = DataInputOutputUtilRt.readSeq(dataStream) {
        val name = StringRef.toString(dataStream.readName())
        val isPublic = dataStream.readBoolean()
        R6ClassMethod(name, isPublic = isPublic)
      }

      val activeBindings = DataInputOutputUtilRt.readSeq(dataStream) {
        val name = StringRef.toString(dataStream.readName())
        val isPublic = dataStream.readBoolean()
        R6ClassActiveBinding(name)
      }

      return R6ClassInfo(className, packageName, superClass, fields, methods, activeBindings)
    }
  }
}