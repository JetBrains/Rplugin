/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.classes.common.context

import com.intellij.r.psi.psi.api.RCallExpression
import com.intellij.r.psi.psi.api.RPsiElement

interface LibraryClassContext {
  val functionName: String
  val functionCall: RCallExpression
  val originalElement: RPsiElement
}