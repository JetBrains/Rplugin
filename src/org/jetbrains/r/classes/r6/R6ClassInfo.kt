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
data class R6ClassMember(override val name: String, val isPublic: Boolean = true) : IR6ClassMember
data class R6ClassActiveBinding(override val name: String) : IR6ClassMember

data class R6ClassInfo(val className: String,
                       val superClasses: List<String>,
                       val members: List<R6ClassMember>,
                       val activeBindings: List<R6ClassActiveBinding>) {

  fun containsMember(memberName: String) : Boolean {
    return (members.map { it.name }.contains(memberName) ||
            activeBindings.map { it.name }.contains(memberName))
  }

  fun serialize(dataStream: StubOutputStream) {
    dataStream.writeName(className)
    DataInputOutputUtilRt.writeSeq(dataStream, superClasses) { dataStream.writeName(it) }

    DataInputOutputUtilRt.writeSeq(dataStream, members) {
      dataStream.writeName(it.name);
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

      val members = DataInputOutputUtilRt.readSeq(dataStream) {
        val name = StringRef.toString(dataStream.readName())
        val isPublic = dataStream.readBoolean()
        R6ClassMember(name, isPublic)
      }

      val activeBindings = DataInputOutputUtilRt.readSeq(dataStream) {
        val name = StringRef.toString(dataStream.readName())
        R6ClassActiveBinding(name)
      }

      return R6ClassInfo(className, superClasses, members, activeBindings)
    }
  }
}

class R6ClassKeywordsProvider {
  companion object {
    val predefinedClassMethods = listOf(
      R6ClassMember("clone", true)
    )

    val visibilityModifiers = listOf(
      R6ClassInfoUtil.argumentPrivate,
      R6ClassInfoUtil.argumentPublic,
      R6ClassInfoUtil.argumentActive,
    )
  }
}