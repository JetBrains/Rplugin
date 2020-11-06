/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.classes.s4

import com.intellij.openapi.util.io.DataInputOutputUtilRt
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef

data class RS4ClassSlot(val name: String, val type: String)

data class RS4ClassInfo(val className: String,
                        val packageName: String,
                        val slots: List<RS4ClassSlot>,
                        val superClasses: List<String>,
                        val isVirtual: Boolean) {
  fun serialize(dataStream: StubOutputStream) {
    dataStream.writeName(className)
    dataStream.writeName(packageName)
    DataInputOutputUtilRt.writeSeq(dataStream, slots) { dataStream.writeName(it.name); dataStream.writeName(it.type) }
    DataInputOutputUtilRt.writeSeq(dataStream, superClasses) { dataStream.writeName(it) }
    dataStream.writeBoolean(isVirtual)
  }

  companion object {
    fun deserialize(dataStream: StubInputStream): RS4ClassInfo {
      val className = StringRef.toString(dataStream.readName())
      val packageName = StringRef.toString(dataStream.readName())
      val slots = DataInputOutputUtilRt.readSeq(dataStream) {
        val name = StringRef.toString(dataStream.readName())
        val type = StringRef.toString(dataStream.readName())
        RS4ClassSlot(name, type)
      }
      val superClasses = DataInputOutputUtilRt.readSeq(dataStream) { StringRef.toString(dataStream.readName()) }
      val isVirtual = dataStream.readBoolean()
      return RS4ClassInfo(className, packageName, slots, superClasses, isVirtual)
    }
  }
}