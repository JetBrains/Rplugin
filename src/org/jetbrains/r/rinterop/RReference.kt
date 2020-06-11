/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.openapi.Disposable
import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.concurrency.isPending
import org.jetbrains.r.debugger.RSourcePosition
import org.jetbrains.r.debugger.exception.RDebuggerException
import org.jetbrains.r.util.thenCancellable
import org.jetbrains.r.util.tryRegisterDisposable

open class RReference internal constructor(internal val proto: Service.RRef, internal val rInterop: RInterop) {
  fun createVariableLoader(): RVariableLoader {
    return RVariableLoader(this)
  }

  fun getMemberRef(name: String): RReference {
    return RReference(ProtoUtil.envMemberRefProto(proto, name), rInterop)
  }

  fun getListElementRef(index: Long): RReference {
    return RReference(ProtoUtil.listElementRefProto(proto, index), rInterop)
  }

  fun getAttributesRef(): RReference {
    return RReference(ProtoUtil.attributesRefProto(proto), rInterop)
  }

  fun copyToPersistentRef(disposableParent: Disposable? = null): CancellablePromise<RPersistentRef> {
    return rInterop.executeAsync(rInterop.asyncStub::copyToPersistentRef, proto)
      .thenCancellable {
        if (it.responseCase == Service.CopyToPersistentRefResponse.ResponseCase.PERSISTENTINDEX) {
          return@thenCancellable RPersistentRef(it.persistentIndex, rInterop, disposableParent)
        }
        throw RDebuggerException(it.error)
      }
      .also { disposableParent?.tryRegisterDisposable(Disposable { if (it.isPending) it.cancel() }) }
  }

  fun getValueInfo(): RValue {
    return ProtoUtil.rValueFromProto(rInterop.executeWithCheckCancel(rInterop.asyncStub::loaderGetValueInfo, proto))
  }

  fun getValueInfoAsync(): CancellablePromise<RValue> {
    return rInterop.executeAsync(rInterop.asyncStub::loaderGetValueInfo, proto).thenCancellable {
      ProtoUtil.rValueFromProto(it)
    }
  }

  fun evaluateAsTextAsync(): CancellablePromise<String> {
    return rInterop.executeAsync(rInterop.asyncStub::evaluateAsText, proto).thenCancellable {
      if (it.resultCase == Service.StringOrError.ResultCase.VALUE) {
        it.value
      } else {
        throw RDebuggerException(it.error)
      }
    }
  }

  fun getDistinctStrings(): List<String> {
    return try {
      rInterop.executeWithCheckCancel(rInterop.asyncStub::getDistinctStrings, proto).listList
    } catch (e: RInteropTerminated) {
      emptyList()
    }
  }

  fun ls(): List<String> {
    return try {
      rInterop.executeWithCheckCancel(rInterop.asyncStub::loadObjectNames, proto).listList
    } catch (e: RInteropTerminated) {
      emptyList()
    }
  }

  fun functionSourcePosition(): RSourcePosition? {
    return try {
      functionSourcePositionAsync().getWithCheckCanceled()
    } catch (e: RInteropTerminated) {
      null
    }
  }

  fun functionSourcePositionAsync(): CancellablePromise<RSourcePosition?> {
    return rInterop.sourceFileManager.getFunctionPosition(this)
  }

  fun getEqualityObject(): Long {
    return rInterop.executeWithCheckCancel(rInterop.asyncStub::getEqualityObject, proto).value
  }

  fun setValue(value: RReference): CancellablePromise<Unit> {
    val request = Service.SetValueRequest.newBuilder()
      .setRef(proto)
      .setValue(value.proto)
      .build()
    return rInterop.executeAsync(rInterop.asyncStub::setValue, request).thenCancellable {
      if (it.responseCase == Service.SetValueResponse.ResponseCase.ERROR) {
        throw RDebuggerException(it.error)
      }
    }
  }

  fun canSetValue() = ProtoUtil.canSetValue(proto)

  companion object {
    fun expressionRef(code: String, env: RReference) = RReference(ProtoUtil.expressionRefProto(code, env.proto), env.rInterop)
    fun expressionRef(code: String, rInterop: RInterop) = expressionRef(code, rInterop.currentEnvRef)

    internal fun sysFrameRef(index: Int, rInterop: RInterop) = RReference(ProtoUtil.sysFrameRefProto(index), rInterop)
    internal fun errorStackSysFrameRef(index: Int, rInterop: RInterop) = RReference(ProtoUtil.errorStackSysFrameRefProto(index), rInterop)
  }
}

class RPersistentRef internal constructor(index: Int, rInterop: RInterop, disposableParent: Disposable? = null):
  RReference(Service.RRef.newBuilder().setPersistentIndex(index).build(), rInterop), Disposable {
  init {
    disposableParent?.tryRegisterDisposable(this)
  }

  override fun dispose() {
    if (!rInterop.isAlive) return
    rInterop.executeAsync(rInterop.asyncStub::disposePersistentRefs, Service.PersistentRefList.newBuilder().addIndices(proto.persistentIndex).build())
  }
}
