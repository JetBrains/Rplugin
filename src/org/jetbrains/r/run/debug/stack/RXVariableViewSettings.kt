/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug.stack

data class RXVariableViewSettings(var showHiddenVariables: Boolean = false,
                                  var showClasses: Boolean = false,
                                  var showSize: Boolean = false)
