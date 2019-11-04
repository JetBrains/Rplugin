/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2011-present Greg Shrago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.r.refactoring.inline

import com.intellij.BundleBase
import com.intellij.openapi.project.Project
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiReference
import com.intellij.refactoring.inline.InlineOptionsDialog
import com.intellij.usageView.UsageViewNodeTextLocation
import org.jetbrains.r.psi.api.RAssignmentStatement

class RInlineAssignmentDialog internal constructor(project: Project,
                                                   private val variable: RAssignmentStatement,
                                                   private val reference: PsiReference?) : InlineOptionsDialog(project, true, variable) {


  init {
    myInvokedOnReference = reference != null
    title = "Inline Expression"
    init()
  }

  override fun getNameLabelText(): String {
    return ElementDescriptionUtil.getElementDescription(myElement, UsageViewNodeTextLocation.INSTANCE)
  }

  override fun getBorderTitle(): String {
    return "Inline"
  }

  override fun getInlineThisText(): String {
    return BundleBase.replaceMnemonicAmpersand("&This reference only and keep the expression")
  }

  override fun getInlineAllText(): String {
    return BundleBase.replaceMnemonicAmpersand("&All references and remove the expression")
  }

  override fun isInlineThis(): Boolean {
    return false
  }

  override fun doAction() {
    invokeRefactoring(RInlineAssignmentProcessor(variable, project, reference, isInlineThisOnly))
  }

  override fun doHelpAction() {}
}
