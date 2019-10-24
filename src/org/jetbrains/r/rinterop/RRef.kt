/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer

open class RRef internal constructor(internal val proto: Service.RRef, internal val rInterop: RInterop) {
  fun createVariableLoader(): RVariableLoader {
    return RVariableLoader(this)
  }

  fun getMemberRef(name: String): RRef {
    return RRef(ProtoUtil.envMemberRefProto(proto, name), rInterop)
  }

  fun getListElementRef(index: Int): RRef {
    return RRef(ProtoUtil.listElementRefProto(proto, index), rInterop)
  }

  fun copyToPersistentRef(disposableParent: Disposable? = null): RPersistentRef {
    return RPersistentRef(rInterop.execute(rInterop.stub::copyToPersistentRef, proto).value, rInterop, disposableParent)
  }

  fun getValueInfo(): RValue {
    return ProtoUtil.rValueFromProto(rInterop.executeWithCheckCancel(rInterop.asyncStub::loaderGetValueInfo, proto))
  }

  fun evaluateAsText(): String {
    return rInterop.executeWithCheckCancel(rInterop.asyncStub::evaluateAsText, proto).value
  }

  fun evaluateAsBoolean(): Boolean {
    return rInterop.executeWithCheckCancel(rInterop.asyncStub::evaluateAsBoolean, proto).value
  }

  fun evaluateAsStringList(): List<String> {
    return rInterop.executeWithCheckCancel(rInterop.asyncStub::evaluateAsStringList, proto).listList
  }

  fun ls(): List<String> {
    return rInterop.executeWithCheckCancel(rInterop.asyncStub::loadObjectNames, proto).listList
  }

  companion object {
    fun expressionRef(code: String, env: RRef) = RRef(ProtoUtil.expressionRefProto(code, env.proto), env.rInterop)
    fun expressionRef(code: String, rInterop: RInterop) = expressionRef(code, rInterop.currentEnvRef)

    fun sysFrameRef(index: Int, rInterop: RInterop) = RRef(ProtoUtil.sysFrameRefProto(index), rInterop)
  }
}

class RPersistentRef internal constructor(index: Int, rInterop: RInterop, disposableParent: Disposable? = null):
  RRef(Service.RRef.newBuilder().setPersistentIndex(index).build(), rInterop), Disposable {
  init {
    disposableParent?.let { Disposer.register(it, this) }
  }

  override fun dispose() {
    rInterop.executeAsync(rInterop.asyncStub::disposePersistentRefs, Service.PersistentRefList.newBuilder().addIndices(proto.persistentIndex).build())
  }
}
