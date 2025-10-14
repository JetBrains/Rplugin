/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.rinterop

import com.intellij.openapi.Disposable
import com.intellij.r.psi.debugger.RSourcePosition
import com.intellij.r.psi.debugger.exception.RDebuggerException
import com.intellij.r.psi.util.thenCancellable
import com.intellij.r.psi.util.tryRegisterDisposable
import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.concurrency.isPending

open class RReference (val proto: RRef, val rInterop: RInterop) {
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
    return rInterop.copyToPersistentRef(proto)
      .thenCancellable {
        if (it.responseCase == CopyToPersistentRefResponse.ResponseCase.PERSISTENTINDEX) {
          return@thenCancellable RPersistentRef(it.persistentIndex, rInterop, disposableParent)
        }
        throw RDebuggerException(it.error)
      }
      .also { disposableParent?.tryRegisterDisposable(Disposable { if (it.isPending) it.cancel() }) }
  }

  fun getValueInfo(): RValue {
    return ProtoUtil.rValueFromProto(rInterop.loaderGetValueInfo(proto).getWithCheckCanceled())
  }

  fun getValueInfoAsync(): CancellablePromise<RValue> {
    return rInterop.loaderGetValueInfo(proto).thenCancellable {
      ProtoUtil.rValueFromProto(it)
    }
  }

  fun evaluateAsTextAsync(): CancellablePromise<String> {
    return rInterop.evaluateAsText(proto).thenCancellable {
      if (it.resultCase == StringOrError.ResultCase.VALUE) {
        it.value
      } else {
        throw RDebuggerException(it.error)
      }
    }
  }

  fun getDistinctStrings(): List<String> {
    return try {
      rInterop.getDistinctStrings(proto).getWithCheckCanceled().listList
    } catch (e: RInteropTerminated) {
      emptyList()
    }
  }

  fun ls(): List<String> {
    return try {
      rInterop.loadObjectNames(proto).getWithCheckCanceled().listList
    } catch (e: RInteropTerminated) {
      emptyList()
    }
  }

  fun functionSourcePosition(): RSourcePosition? = functionSourcePositionWithText()?.first

  fun functionSourcePositionAsync(): CancellablePromise<RSourcePosition?> =
    functionSourcePositionWithTextAsync().thenCancellable { it?.first }

  fun functionSourcePositionWithText(): Pair<RSourcePosition, String?>? {
    return try {
      functionSourcePositionWithTextAsync().getWithCheckCanceled()
    } catch (e: RInteropTerminated) {
      null
    }
  }

  fun functionSourcePositionWithTextAsync(): CancellablePromise<Pair<RSourcePosition, String?>?> {
    return rInterop.getFunctionPosition(this)
  }

  fun getEqualityObject(): Long {
    return rInterop.getEqualityObject(proto).getWithCheckCanceled().value
  }

  fun setValue(value: RReference): CancellablePromise<RValue> {
    val request = SetValueRequest.newBuilder()
      .setRef(proto)
      .setValue(value.proto)
      .build()
    return rInterop.setValue(request).thenCancellable {
      if (it.hasError()) {
        throw RDebuggerException(it.error.text)
      }
      ProtoUtil.rValueFromProto(it)
    }
  }

  fun canSetValue() = ProtoUtil.canSetValue(proto)

  companion object {
    fun expressionRef(code: String, env: RReference): RReference = RReference(ProtoUtil.expressionRefProto(code, env.proto), env.rInterop)
    fun expressionRef(code: String, rInterop: RInterop): RReference = expressionRef(code, rInterop.currentEnvRef)

    fun sysFrameRef(index: Int, rInterop: RInterop): RReference = RReference(ProtoUtil.sysFrameRefProto(index), rInterop)
    fun errorStackSysFrameRef(index: Int, rInterop: RInterop): RReference = RReference(ProtoUtil.errorStackSysFrameRefProto(index), rInterop)
  }
}

class RPersistentRef(index: Int, rInterop: RInterop, disposableParent: Disposable? = null):
  RReference(RRef.newBuilder().setPersistentIndex(index).build(), rInterop), Disposable {
  init {
    disposableParent?.tryRegisterDisposable(this)
  }

  override fun dispose() {
    if (!rInterop.isAlive) return
    rInterop.disposePersistentRefs(PersistentRefList.newBuilder().addIndices(proto.persistentIndex).build())
  }
}
