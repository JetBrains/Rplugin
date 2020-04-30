/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

sealed class RInteropException(message: String, cause: Throwable? = null) : Exception(message, cause)
class RInteropTerminated : RInteropException("RWrapper was terminated")
class RInteropRequestFailed(methodName: String, cause: Throwable? = null) : RInteropException("Request $methodName failed", cause)
