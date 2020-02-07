/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

object ProtoUtil {
  fun rValueFromProto(proto: Service.ValueInfo): RValue {
    return when {
      proto.hasValue() -> RValueSimple(proto.value.textValue.trimEnd(), proto.value.isComplete, proto.value.isVector)
      proto.hasList() -> RValueList(proto.list.length)
      proto.hasDataFrame() -> RValueDataFrame(proto.dataFrame.rows, proto.dataFrame.cols)
      proto.hasUnevaluated() -> RValueUnevaluated(proto.unevaluated.code.trimEnd())
      proto.hasFunction() -> RValueFunction(proto.function.header.trimEnd())
      proto.hasEnvironment() -> RValueEnvironment(proto.environment.name)
      proto.hasGraph() -> RValueGraph
      proto.hasError() -> RValueError(proto.error.text.trimEnd())
      else -> RValueSimple("")
    }
  }

  fun envMemberRefProto(env: Service.RRef, name: String): Service.RRef {
    return Service.RRef.newBuilder().setMember(Service.RRef.Member.newBuilder().setEnv(env).setName(name)).build()
  }

  fun listElementRefProto(list: Service.RRef, index: Int): Service.RRef {
    return Service.RRef.newBuilder().setListElement(Service.RRef.ListElement.newBuilder().setList(list).setIndex(index)).build()
  }

  fun parentEnvRefProto(env: Service.RRef, index: Int): Service.RRef {
    return Service.RRef.newBuilder().setParentEnv(Service.RRef.ParentEnv.newBuilder().setEnv(env).setIndex(index)).build()
  }

  fun expressionRefProto(code: String, env: Service.RRef): Service.RRef {
    return Service.RRef.newBuilder().setExpression(Service.RRef.Expression.newBuilder().setEnv(env).setCode(code)).build()
  }

  fun sysFrameRefProto(index: Int): Service.RRef {
    return Service.RRef.newBuilder().setSysFrameIndex(index).build()
  }

  fun errorStackSysFrameRefProto(index: Int): Service.RRef {
    return Service.RRef.newBuilder().setErrorStackSysFrameIndex(index).build()
  }

  fun getVariablesRequestProto(obj: Service.RRef, start: Int, end: Int): Service.GetVariablesRequest {
    return Service.GetVariablesRequest.newBuilder().setObj(obj).setStart(start).setEnd(end).build()
  }

  fun canSetValue(ref: Service.RRef): Boolean = when (ref.refCase) {
    Service.RRef.RefCase.MEMBER -> true
    Service.RRef.RefCase.LISTELEMENT -> canSetValue(ref.listElement.list)
    else -> false
  }
}