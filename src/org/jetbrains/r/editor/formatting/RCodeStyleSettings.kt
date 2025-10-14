// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor.formatting

import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import com.intellij.r.psi.RLanguage

@Suppress("PropertyName")
class RCodeStyleSettings(container: CodeStyleSettings) : CustomCodeStyleSettings(RLanguage.INSTANCE.id, container) {
  @JvmField
  var SPACE_BEFORE_LEFT_BRACKET: Boolean = false
  @JvmField
  var SPACE_BEFORE_REPEAT_LBRACE: Boolean = true
  @JvmField
  var SPACE_AROUND_BINARY_TILDE_OPERATOR: Boolean = true
  @JvmField
  var SPACE_AROUND_DISJUNCTION_OPERATORS: Boolean = true
  @JvmField
  var SPACE_AROUND_CONJUNCTION_OPERATORS: Boolean = true
  @JvmField
  var SPACE_AROUND_INFIX_OPERATOR: Boolean = true
  @JvmField
  var SPACE_AROUND_COLON_OPERATOR: Boolean = false
  @JvmField
  var SPACE_AROUND_EXPONENTIATION_OPERATOR: Boolean = false
  @JvmField
  var SPACE_AROUND_SUBSET_OPERATOR: Boolean = false
  @JvmField
  var SPACE_AROUND_AT_OPERATOR: Boolean = false

  @JvmField
  var ALIGN_ASSIGNMENT_OPERATORS: Boolean = false
  @JvmField
  var ALIGN_COMMENTS: Boolean = false
}
