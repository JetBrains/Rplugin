/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring

import com.intellij.lang.refactoring.RefactoringSupportProvider
import org.jetbrains.r.refactoring.extractmethod.RExtractMethodHandler

class RRefactoringProvider : RefactoringSupportProvider() {
  override fun getIntroduceVariableHandler() = RIntroduceVariableHandler()
  override fun getIntroduceParameterHandler() = RIntroduceParameterHandler()
  override fun getExtractMethodHandler() = RExtractMethodHandler()
}
