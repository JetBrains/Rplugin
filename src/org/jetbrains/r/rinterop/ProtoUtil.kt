/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

object ProtoUtil {
  fun rValueFromProto(proto: Service.ValueInfo): RValue {
    return when {
      proto.hasValue() -> RValueSimple(proto.clsList, proto.value.textValue.trimEnd(),
                                       proto.value.isComplete, proto.value.isVector, proto.value.isS4)
      proto.hasList() -> RValueList(proto.clsList, proto.list.length)
      proto.hasDataFrame() -> RValueDataFrame(proto.clsList, proto.dataFrame.rows, proto.dataFrame.cols)
      proto.hasUnevaluated() -> RValueUnevaluated(proto.clsList, proto.unevaluated.code.trimEnd())
      proto.hasFunction() -> RValueFunction(proto.clsList, proto.function.header.trimEnd())
      proto.hasEnvironment() -> RValueEnvironment(proto.clsList, proto.environment.name)
      proto.hasGraph() -> RValueGraph(proto.clsList)
      proto.hasMatrix() -> RValueMatrix(proto.clsList, proto.matrix.dimList)
      proto.hasError() -> RValueError(proto.clsList, proto.error.text.trimEnd())
      else -> RValueSimple(listOf(), "")
    }
  }

  fun envMemberRefProto(env: Service.RRef, name: String): Service.RRef {
    return Service.RRef.newBuilder().setMember(Service.RRef.Member.newBuilder().setEnv(env).setName(name)).build()
  }

  fun listElementRefProto(list: Service.RRef, index: Long): Service.RRef {
    return Service.RRef.newBuilder().setListElement(Service.RRef.ListElement.newBuilder().setList(list).setIndex(index)).build()
  }

  fun attributesRefProto(x: Service.RRef): Service.RRef {
    return Service.RRef.newBuilder().setAttributes(x).build()
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

  fun getVariablesRequestProto(obj: Service.RRef, start: Long, end: Long): Service.GetVariablesRequest {
    return Service.GetVariablesRequest.newBuilder().setObj(obj).setStart(start).setEnd(end).build()
  }

  fun canSetValue(ref: Service.RRef): Boolean = when (ref.refCase) {
    Service.RRef.RefCase.MEMBER -> true
    Service.RRef.RefCase.LISTELEMENT -> canSetValue(ref.listElement.list)
    Service.RRef.RefCase.ATTRIBUTES -> canSetValue(ref.attributes)
    else -> false
  }
}