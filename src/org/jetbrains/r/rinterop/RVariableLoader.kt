/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.r.util.thenCancellable

class RVariableLoader internal constructor(val obj: RRef) {
  val rInterop = obj.rInterop

  val parentEnvironments = rInterop.AsyncCached(emptyList()) {
    rInterop.executeAsync(rInterop.asyncStub::loaderGetParentEnvs, obj.proto).then { response ->
      response.envsList.mapIndexed { index, it ->
        REnvironmentRef(it.name, RRef(ProtoUtil.parentEnvRefProto(obj.proto, index + 1), rInterop))
      }
    }
  }

  val variablesAsync = rInterop.AsyncCached<List<RVar>>(emptyList()) {
    loadVariablesPartially(0, -1).thenCancellable { it.vars }
  }
  val variables get() = variablesAsync.getWithCheckCancel()

  data class VariablesPart(val vars: List<RVar>, val totalCount: Long)

  fun loadVariablesPartially(start: Long, end: Long): CancellablePromise<VariablesPart> {
    return rInterop.executeAsync(rInterop.asyncStub::loaderGetVariables,
                                 ProtoUtil.getVariablesRequestProto(obj.proto, start, end))
      .then(rInterop.executor) { response ->
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
}
