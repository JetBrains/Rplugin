/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.s4.context

import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RPsiElement

interface RS4Context {
  val contextFunctionName: String
  val contextFunctionCall: RCallExpression
  val originalElement: RPsiElement
}