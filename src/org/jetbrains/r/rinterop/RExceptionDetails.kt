/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.r.psi.rinterop.ExceptionInfo

sealed class RExceptionDetails

object RInterrupted : RExceptionDetails()

class RNoSuchPackageError(val packageName: String) : RExceptionDetails()

data class RExceptionInfo(val message: String, val call: String?, val details: RExceptionDetails?)

internal fun exceptionInfoFromProto(proto: ExceptionInfo): RExceptionInfo {
  val details = when (proto.detailsCase) {
    ExceptionInfo.DetailsCase.INTERRUPTED -> RInterrupted
    ExceptionInfo.DetailsCase.PACKAGENOTFOUND -> RNoSuchPackageError(proto.packageNotFound)
    else -> null
  }
  return RExceptionInfo(proto.message, proto.call.takeIf { it.isNotEmpty() }, details)
}
