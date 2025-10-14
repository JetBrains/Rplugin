/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.r.psi.roxygen.psi

import com.intellij.psi.tree.IElementType
import com.intellij.r.psi.roxygen.RoxygenLanguage
import org.jetbrains.annotations.NonNls

class RoxygenElementType(@NonNls debugName: String) : IElementType(debugName, RoxygenLanguage.INSTANCE)