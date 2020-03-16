/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

class RVariableLoader internal constructor(val obj: RRef) {
  val rInterop = obj.rInterop

  val parentEnvironments: List<REnvironmentRef> by rInterop.Cached {
    rInterop.executeWithCheckCancel(rInterop.asyncStub::loaderGetParentEnvs, obj.proto).envsList.mapIndexed { index, it ->
      REnvironmentRef(it.name, RRef(ProtoUtil.parentEnvRefProto(obj.proto, index + 1), rInterop))
    }
  }

  val variables: List<RVar> by rInterop.Cached {
    loadVariablesPartially(0, -1).vars
  }

  data class VariablesPart(val vars: List<RVar>, val totalCount: Long)

  fun loadVariablesPartially(start: Long, end: Long): VariablesPart {
    val response = rInterop.executeWithCheckCancel(rInterop.asyncStub::loaderGetVariables,
                                                   ProtoUtil.getVariablesRequestProto(obj.proto, start, end))
    val vars = if (response.isEnv) {
      response.varsList.map { RVar(it.name, obj.getMemberRef(it.name), ProtoUtil.rValueFromProto(it.value)) }
    } else {
      response.varsList.mapIndexed { index, it ->
        RVar(it.name, obj.getListElementRef(start + index.toLong()), ProtoUtil.rValueFromProto(it.value))
      }
    }
    return VariablesPart(vars, response.totalCount)
  }
}
