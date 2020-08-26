/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.debugger

import com.intellij.openapi.util.TextRange
import org.jetbrains.r.rinterop.RReference

data class RStackFrame(val position: RSourcePosition?, val environment: RReference, val functionName: String?, val equalityObject: Long,
                       val extendedPosition: TextRange? = null, val sourcePositionText: String? = null)
