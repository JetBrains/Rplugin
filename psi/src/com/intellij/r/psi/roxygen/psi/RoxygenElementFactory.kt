/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.r.psi.roxygen.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.r.psi.roxygen.RoxygenLanguage

object RoxygenElementFactory {
  fun buildRoxygenFileFromText(project: Project?, text: String): PsiFile? {
    return PsiFileFactory.getInstance(project).createFileFromText("a.R", RoxygenLanguage.INSTANCE, text)
  }
}