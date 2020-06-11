/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

object ProtoUtil {
  fun rValueFromProto(proto: ValueInfo): RValue {
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
      proto.hasError() -> RValueError(proto.error.text.trimEnd())
      else -> RValueSimple(listOf(), "")
    }
  }

  fun envMemberRefProto(env: RRef, name: String): RRef {
    return RRef.newBuilder().setMember(RRef.Member.newBuilder().setEnv(env).setName(name)).build()
  }

  fun listElementRefProto(list: RRef, index: Long): RRef {
    return RRef.newBuilder().setListElement(RRef.ListElement.newBuilder().setList(list).setIndex(index)).build()
  }

  fun attributesRefProto(x: RRef): RRef {
    return RRef.newBuilder().setAttributes(x).build()
  }

  fun parentEnvRefProto(env: RRef, index: Int): RRef {
    return RRef.newBuilder().setParentEnv(RRef.ParentEnv.newBuilder().setEnv(env).setIndex(index)).build()
  }

  fun expressionRefProto(code: String, env: RRef): RRef {
    return RRef.newBuilder().setExpression(RRef.Expression.newBuilder().setEnv(env).setCode(code)).build()
  }

  fun sysFrameRefProto(index: Int): RRef {
    return RRef.newBuilder().setSysFrameIndex(index).build()
  }

  fun errorStackSysFrameRefProto(index: Int): RRef {
    return RRef.newBuilder().setErrorStackSysFrameIndex(index).build()
  }

  fun canSetValue(ref: RRef): Boolean = when (ref.refCase) {
    RRef.RefCase.MEMBER -> true
    RRef.RefCase.LISTELEMENT -> canSetValue(ref.listElement.list)
    RRef.RefCase.ATTRIBUTES -> canSetValue(ref.attributes)
    else -> false
  }
}