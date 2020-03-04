/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

sealed class RExceptionDetails

object RInterrupted : RExceptionDetails()

internal fun exceptionInfoFromProto(proto: Service.ExceptionInfo): Pair<String, RExceptionDetails?> {
  val details = when (proto.detailsCase) {
    Service.ExceptionInfo.DetailsCase.INTERRUPTED -> RInterrupted
    else -> null
  }
  return proto.message to details
}
