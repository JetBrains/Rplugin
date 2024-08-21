/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.TestOnly

abstract class TestableCreateFileFromTemplateAction : CreateFileFromTemplateAction() {

  @TestOnly
  fun createTestFile(name: String?, templateName: String, directory: PsiDirectory): PsiFile? {
    return createFile(name, templateName, directory)
  }
}
