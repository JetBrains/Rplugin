/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.ExceptionWithAttachments

sealed class RInteropException(val rInterop: RInterop, message: String, cause: Throwable? = null) :
  Exception(message, cause), ExceptionWithAttachments {
  private val lazyAttachments by lazy {
    val log = rInterop.rInteropGrpcLogger.toJson(true)
    arrayOf(Attachment("grpc_log.json", log).apply { isIncluded = true })
  }

  override fun getAttachments() = lazyAttachments
}

class RInteropTerminated(rInterop: RInterop) : RInteropException(rInterop, "RWrapper was terminated")
class RInteropRequestFailed(rInterop: RInterop, methodName: String, cause: Throwable? = null) :
  RInteropException(rInterop, "Request $methodName failed", cause)
