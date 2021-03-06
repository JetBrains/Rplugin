/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.s4.methods

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef

data class RS4MethodParameterInfo(val name: String, val type: String) {
  fun serialize(dataStream: StubOutputStream) {
    dataStream.writeName(name)
    dataStream.writeName(type)
  }

  companion object {
    fun deserialize(dataStream: StubInputStream): RS4MethodParameterInfo {
      val name = StringRef.toString(dataStream.readName())
      val type = StringRef.toString(dataStream.readName())
      return RS4MethodParameterInfo(name, type)
    }
  }
}