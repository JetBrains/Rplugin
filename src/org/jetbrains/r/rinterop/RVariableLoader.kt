/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.concurrency.resolvedCancellablePromise
import org.jetbrains.r.util.thenAsyncCancellable
import org.jetbrains.r.util.thenCancellable

class RVariableLoader internal constructor(val obj: RReference) {
  val rInterop = obj.rInterop

  val parentEnvironments = rInterop.AsyncCached(emptyList()) {
    rInterop.executeAsync(rInterop.asyncStub::loaderGetParentEnvs, obj.proto).thenCancellable { response ->
      response.envsList.mapIndexed { index, it ->
        REnvironmentRef(it.name, RReference(ProtoUtil.parentEnvRefProto(obj.proto, index + 1), rInterop))
      }
    }
  }

  private val variablesAsync = rInterop.AsyncCached<List<RVar>>(emptyList()) {
    loadVariablesPartially(0, BLOCK_SIZE).thenAsyncCancellable<VariablesPart, List<RVar>> {
      if (it.totalCount <= BLOCK_SIZE) {
        return@thenAsyncCancellable resolvedCancellablePromise(it.vars)
      }
      val result = it.vars.toMutableList()
      var promise = resolvedCancellablePromise(Unit)
      var offset = BLOCK_SIZE
      while (offset < it.totalCount) {
        val currentOffset = offset
        offset += BLOCK_SIZE
        promise = promise.thenAsyncCancellable {
          loadVariablesPartially(currentOffset, currentOffset + BLOCK_SIZE).thenCancellable { part ->
            result.addAll(part.vars)
            Unit
          }
        }
      }
      promise.thenCancellable { result }
    }
  }
  val variables get() = variablesAsync.safeGet()

  data class VariablesPart(val vars: List<RVar>, val totalCount: Long)

  fun loadVariablesPartially(
    start: Long, end: Long, withHidden: Boolean = true,
    noFunctions: Boolean = false, onlyFunctions: Boolean = false): CancellablePromise<VariablesPart> {
    val request = GetVariablesRequest.newBuilder()
      .setObj(obj.proto).setStart(start).setEnd(end)
      .setNoHidden(!withHidden).setNoFunctions(noFunctions).setOnlyFunctions(onlyFunctions).build()
    return rInterop.executeAsync(rInterop.asyncStub::loaderGetVariables, request)
      .thenCancellable { response ->
        val vars = if (response.isEnv) {
          response.varsList.map { RVar(it.name, obj.getMemberRef(it.name), ProtoUtil.rValueFromProto(it.value)) }
        } else {
          response.varsList.mapIndexed { index, it ->
            RVar(it.name, obj.getListElementRef(start + index.toLong()), ProtoUtil.rValueFromProto(it.value))
          }
        }
        VariablesPart(vars, response.totalCount)
      }
  }

  companion object {
    private const val BLOCK_SIZE = 500L
  }
}
