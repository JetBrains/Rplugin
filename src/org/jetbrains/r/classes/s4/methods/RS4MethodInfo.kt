/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.s4.methods

import com.intellij.openapi.util.io.DataInputOutputUtilRt
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.r.classes.s4.methods.RS4MethodsUtil.associatedS4GenericInfo
import org.jetbrains.r.classes.s4.methods.RS4MethodsUtil.toS4MethodParameters
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.stubs.RS4GenericIndex
import java.util.concurrent.ConcurrentHashMap

data class RS4GenericSignature(val parameters: List<String>, val valueClasses: List<String>, val partialParsed: Boolean) {
  fun serialize(dataStream: StubOutputStream) {
    DataInputOutputUtilRt.writeSeq(dataStream, parameters) { dataStream.writeName(it) }
    DataInputOutputUtilRt.writeSeq(dataStream, valueClasses) { dataStream.writeName(it) }
    dataStream.writeBoolean(partialParsed)
  }

  companion object {
    fun deserialize(dataStream: StubInputStream): RS4GenericSignature {
      val parameters = DataInputOutputUtilRt.readSeq(dataStream) { dataStream.readNameString()!! }
      val valueClasses = DataInputOutputUtilRt.readSeq(dataStream) { dataStream.readNameString()!! }
      val partialParsed = dataStream.readBoolean()
      return RS4GenericSignature(parameters, valueClasses, partialParsed)
    }
  }
}

sealed class RS4GenericOrMethodInfo {
  fun serialize(dataStream: StubOutputStream) {
    dataStream.writeName(this::class.qualifiedName)
    serializeInner(dataStream)
  }

  protected abstract fun serializeInner(dataStream: StubOutputStream)

  companion object {
    fun deserialize(dataStream: StubInputStream): RS4GenericOrMethodInfo = when (val className = dataStream.readNameString()!!) {
      RS4GenericInfo::class.qualifiedName -> RS4GenericInfo.deserialize(dataStream)
      RS4SignatureMethodInfo::class.qualifiedName -> RS4SignatureMethodInfo.deserialize(dataStream)
      RS4RawMethodInfo::class.qualifiedName -> RS4RawMethodInfo.deserialize(dataStream)
      else -> error("Unknown class: $className")
    }
  }
}

data class RS4GenericInfo(val methodName: String, val signature: RS4GenericSignature) : RS4GenericOrMethodInfo() {
  override fun serializeInner(dataStream: StubOutputStream) {
    dataStream.writeName(methodName)
    signature.serialize(dataStream)
  }

  companion object {
    fun deserialize(dataStream: StubInputStream): RS4GenericInfo {
      val methodName = dataStream.readNameString()!!
      val signature = RS4GenericSignature.deserialize(dataStream)
      return RS4GenericInfo(methodName, signature)
    }
  }
}

abstract class RS4MethodInfo : RS4GenericOrMethodInfo() {
  abstract val methodName: String
  abstract fun getParameters(scope: GlobalSearchScope): List<RS4MethodParameterInfo>
}

data class RS4SignatureMethodInfo(override val methodName: String, private val signature: List<String>?) : RS4MethodInfo() {

  private val parametersCache = ConcurrentHashMap<GlobalSearchScope, List<RS4MethodParameterInfo>>()

  override fun getParameters(scope: GlobalSearchScope): List<RS4MethodParameterInfo> {
    val project = scope.project ?: return emptyList()
    return parametersCache.getOrPut(scope) {
      val generic = RS4GenericIndex.findDefinitionsByName(methodName, project, scope).singleOrNull() ?: return@getOrPut emptyList()
      val info = generic.associatedS4GenericInfo!!
      val defParams = info.signature.parameters
      if (signature == null) {
        return@getOrPut defParams.map { RS4MethodParameterInfo(it, "ANY") }
      }
      val call = RElementFactory.createFuncallFromText(project, "tmp(${signature.joinToString(", ")})")
      val argInfo = RArgumentInfo.getArgumentInfo(call.argumentList.expressionList, info.signature.parameters, project)
      argInfo.toS4MethodParameters(true)
    }
  }

  override fun serializeInner(dataStream: StubOutputStream) {
    dataStream.writeName(methodName)
    val isSignatureExists = signature != null
    dataStream.writeBoolean(isSignatureExists)
    if (isSignatureExists) {
      DataInputOutputUtilRt.writeSeq(dataStream, signature!!) { dataStream.writeName(it) }
    }
  }

  companion object {
    fun deserialize(dataStream: StubInputStream): RS4MethodInfo {
      val methodName = dataStream.readNameString()!!
      val isSignatureExists = dataStream.readBoolean()
      val signature =
        if (isSignatureExists) DataInputOutputUtilRt.readSeq(dataStream) { dataStream.readNameString()!! }
        else null
      return RS4SignatureMethodInfo(methodName, signature)
    }
  }
}

data class RS4RawMethodInfo(override val methodName: String, val parameters: List<RS4MethodParameterInfo>) : RS4MethodInfo() {

  override fun getParameters(scope: GlobalSearchScope): List<RS4MethodParameterInfo> = parameters

  override fun serializeInner(dataStream: StubOutputStream) {
    dataStream.writeName(methodName)
    DataInputOutputUtilRt.writeSeq(dataStream, parameters) { it.serialize(dataStream) }
  }

  companion object {
    fun deserialize(dataStream: StubInputStream): RS4MethodInfo {
      val methodName = dataStream.readNameString()!!
      val parameters = DataInputOutputUtilRt.readSeq(dataStream) { RS4MethodParameterInfo.deserialize(dataStream) }
      return RS4RawMethodInfo(methodName, parameters)
    }
  }
}