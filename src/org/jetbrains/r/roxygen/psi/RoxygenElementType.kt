/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen.psi

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls
import org.jetbrains.r.roxygen.RoxygenLanguage

class RoxygenElementType(@NonNls debugName: String) : IElementType(debugName, RoxygenLanguage.INSTANCE)