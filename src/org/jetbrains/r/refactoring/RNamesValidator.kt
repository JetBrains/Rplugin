/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project

object RNamesValidator : NamesValidator {
  override fun isKeyword(name: String, project: Project?) = isKeyword(name)
  override fun isIdentifier(name: String, project: Project?) = isIdentifier(name)

  fun isKeyword(name: String) = name in RESERVED_WORDS

  fun isIdentifier(name: String) = !isKeyword(name) && name matches IDENTIFIER_REGEX

  fun isOperatorIdentifier(name: String) = name.length >= 2 && name.startsWith('%') && name.endsWith('%')

  fun quoteIfNeeded(name: String) = if (isIdentifier(name)) name else "`${name.replace("\\", "\\\\").replace("`", "\\`")}`"

  private val RESERVED_WORDS = setOf(
    "if", "else", "repeat", "while", "function", "for", "in", "next", "break", "TRUE", "FALSE",
    "NULL", "Inf", "NaN", "NA", "NA_integer_", "NA_real_", "NA_complex_", "NA_character"
  )
  private val IDENTIFIER_REGEX = Regex("^(\\p{Alpha}|\\.)(\\p{Alnum}|_|\\.)*$")
}