/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.refactoring

import com.intellij.lang.LanguageNamesValidation
import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project
import com.intellij.r.psi.RLanguage

val rNamesValidator: NamesValidator
  get() = LanguageNamesValidation.INSTANCE.forLanguage(RLanguage.INSTANCE)

internal class RNamesValidator : NamesValidator {

  override fun isKeyword(name: String, project: Project?): Boolean =
    name in RESERVED_WORDS

  override fun isIdentifier(name: String, project: Project?): Boolean =
    name !in RESERVED_WORDS
    && name matches IDENTIFIER_REGEX
}

private val RESERVED_WORDS = setOf(
  "if", "else", "repeat", "while", "function", "for", "in", "next", "break", "TRUE", "FALSE",
  "NULL", "Inf", "NaN", "NA", "NA_integer_", "NA_real_", "NA_complex_", "NA_character"
)

private val IDENTIFIER_REGEX = Regex("^(\\p{Alpha}|\\.)(\\p{Alnum}|_|\\.)*$")

fun NamesValidator.quoteIfNeeded(
  name: String,
  project: Project? = null,
): String = if (isIdentifier(name, project)) name else "`${name.replace("\\", "\\\\").replace("`", "\\`")}`"