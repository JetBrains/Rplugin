/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

sealed class RExceptionDetails

object RInterrupted : RExceptionDetails()

class RNoSuchPackageError(val packageName: String) : RExceptionDetails()

data class RExceptionInfo(val message: String, val call: String?, val details: RExceptionDetails?)

internal fun exceptionInfoFromProto(proto: Service.ExceptionInfo): RExceptionInfo {
  val details = when (proto.detailsCase) {
    Service.ExceptionInfo.DetailsCase.INTERRUPTED -> RInterrupted
    Service.ExceptionInfo.DetailsCase.PACKAGENOTFOUND -> RNoSuchPackageError(proto.packageNotFound)
    else -> null
  }
  return RExceptionInfo(proto.message, proto.call.takeIf { it.isNotEmpty() }, details)
}
