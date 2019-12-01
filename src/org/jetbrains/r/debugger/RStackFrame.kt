/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.debugger

import org.jetbrains.r.rinterop.RRef

data class RStackFrame(val position: RSourcePosition?, val environment: RRef, val functionName: String?, val equalityObject: Long)
