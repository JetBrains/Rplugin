/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.StringValue
import com.intellij.openapi.project.Project
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.File
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.util.*
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.memberFunctions

class GRPCTester(path: String,
                 private val rInterop: RInterop,
                 private val pathReplacers: ArrayList<PathReplacer>) {

  val messages: Array<RInteropGrpcLogger.Message>

  init {
    val text = XZCompressorInputStream(File(path).inputStream()).use { InputStreamReader(it).readText() }
    messages = GsonBuilder().registerTypeAdapter(RInteropGrpcLogger.Message::class.java, MessageDeserializer)
                            .create()
                            .fromJson<Array<RInteropGrpcLogger.Message>>(text, Array<RInteropGrpcLogger.Message>::class.java)
  }

  fun proceedMessage(message: RInteropGrpcLogger.Message): Pair<Any, Any?>? {
    val function = rInterop.stub::class.memberFunctions.first { it.name == message.methodName }
    return buildRequestAndCall(function, message.methodName, message.request, if (message is RInteropGrpcLogger.StubMessage) message.response else null)
  }

  private fun buildRequestAndCall(function: KFunction<*>,
                                  methodName: String,
                                  requestArray: ByteArray,
                                  responseArray: ByteArray?): Pair<Any, Any?>? {
    val kParameter = function.parameters[1]
    val classifier = kParameter.type.classifier
    val returnClassifier = function.returnType.classifier
    if (classifier is KClass<*> && returnClassifier is KClass<*>) {
      val parseFrom = getParseFrom(classifier)
      val returnParseFrom = getParseFrom(returnClassifier)
      val request = parseFrom!!.call(requestArray).let { request ->
        pathReplacers.mapNotNull { it.replace(methodName, request as GeneratedMessageV3) }.firstOrNull() ?: request
      }
      println("Sending requestArray: " + request.toString())
      return Pair(function.call(rInterop.stub, request)!!, responseArray?.let { returnParseFrom?.call(it) } )
    }
    return null
  }

  private fun getParseFrom(classifier: KClass<*>): KCallable<*>? {
    return classifier.members.firstOrNull {
      it.name == "parseFrom" && it.parameters.size == 1 && it.parameters[0].type.toString().contains("ByteArray")
    }
  }
}

interface PathReplacer {
  fun replace(methodName: String, request: GeneratedMessageV3): GeneratedMessageV3?
}

class InitPathReplacer(private val project: Project, private val rScriptPath: String) : PathReplacer {
  override fun replace(methodName: String, request: GeneratedMessageV3): GeneratedMessageV3? {
    if (methodName != "init" || request !is Init) return null
    return Init.newBuilder().setProjectDir(project.basePath).setRScriptsPath(rScriptPath).build()
  }
}

class HtmlPathReplacer(private val project: Project, private val fileForUrls: String) : PathReplacer {
  override fun replace(methodName: String, request: GeneratedMessageV3): GeneratedMessageV3? {
    if (methodName != "htmlViewerInit") return null
    return StringValue.newBuilder().setValue(fileForUrls).build()
  }
}

object MessageDeserializer : JsonDeserializer<RInteropGrpcLogger.Message> {
  private val decoder = Base64.getDecoder()
  override fun deserialize(el: JsonElement, p1: Type, p2: JsonDeserializationContext): RInteropGrpcLogger.Message {
    val asJsonObject = el.asJsonObject
    return if (asJsonObject.size() < 4) {
      RInteropGrpcLogger.StubMessage(asJsonObject["methodName"].asString,
                                     decoder.decode(asJsonObject["request"].asString),
                                     if (asJsonObject.size() == 2) null else decoder.decode(asJsonObject["response"].asString))
    }
    else {
      RInteropGrpcLogger.CommandMessage(asJsonObject["methodName"].asString, decoder.decode(asJsonObject["request"].asString)).apply {
        stdout.append(asJsonObject["stdout"].asString)
        stderr.append(asJsonObject["stderr"].asString)
      }
    }
  }
}