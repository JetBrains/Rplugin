/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.google.gson.*
import com.google.protobuf.GeneratedMessageV3
import com.intellij.util.containers.ContainerUtil
import io.grpc.MethodDescriptor
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class RInteropGrpcLogger(private val maxMessages: Int? = null) {
  val messages = ArrayDeque<Message>()
  private val commandMessages = ContainerUtil.createConcurrentIntObjectMap<CommandMessage>()
  private val stubMessages = ContainerUtil.createConcurrentIntObjectMap<Pair<ByteArray, String>>()
  private val stubMessageEnumerator: AtomicInteger = AtomicInteger(0)

  fun nextStubNumber(): Int {
    return stubMessageEnumerator.getAndIncrement()
  }

  fun <TRequest : GeneratedMessageV3, TResponse : GeneratedMessageV3>
    onExecuteRequestAsync(number: Int, methodDescriptor: MethodDescriptor<TRequest, TResponse>, request: TRequest) {
    commandMessages.put(number, CommandMessage(methodDescriptor.fullMethodName.let { it.substring(it.indexOf('/') + 1) }, request.toByteArray()))
  }

  fun onExecuteRequestFinish(number: Int) {
    addMessage(commandMessages.remove(number))
  }

  fun onOutputAvailable(number: Int, value: Service.CommandOutput) {

    commandMessages.get(number)?.apply {
      when (value.type) {
        Service.CommandOutput.Type.STDOUT -> stdout.append(value.text.toStringUtf8())
        Service.CommandOutput.Type.STDERR -> stderr.append(value.text.toStringUtf8())
        else -> throw IllegalStateException("Cannot be reach")
      }
    }
  }

  fun onStubMessageRequest(number: Int, message: GeneratedMessageV3, methodName: String) {
    stubMessages.put(number, Pair(message.toByteArray(), methodName))
  }

  fun onStubMessageResponse(number: Int, message: GeneratedMessageV3?) {
    val (bytes, methodName) = stubMessages.remove(number)
    addMessage(StubMessage(methodName, bytes, message?.toByteArray()))
  }

  @Synchronized
  private fun addMessage(it: Message) {
    messages.addLast(it)
    if (maxMessages != null && messages.size > maxMessages) {
      messages.removeFirst()
    }
  }

  interface Message {
    val methodName: String
    val request: ByteArray
  }

  class CommandMessage(override val methodName: String,
                       override val request: ByteArray) : Message {
    val stdout: StringBuffer = StringBuffer()
    val stderr: StringBuffer = StringBuffer()
  }

  class StubMessage(override val methodName: String,
                    override val request: ByteArray,
                    val response: ByteArray?) : Message

  fun toJson(withPending: Boolean = false): String {
    val gsonBuilder = GsonBuilder().registerTypeAdapter(ByteArray::class.java, object : JsonSerializer<ByteArray> {
      override fun serialize(p0: ByteArray?, p1: Type?, p2: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(Base64.getEncoder().encodeToString(p0))
      }
    }).setPrettyPrinting().create()
    return if (withPending) {
      gsonBuilder.toJson(mapOf<String, Any>(
        "messages" to messages,
        "pending" to stubMessages.values().map { StubMessage(it.second, it.first, null) }.toList(),
          "pendingAsync" to commandMessages.values()
      ))
    } else {
      gsonBuilder.toJson(messages)
    }
  }
}