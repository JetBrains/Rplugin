/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.template

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.platform.ProjectTemplate
import com.intellij.platform.ProjectTemplatesFactory
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.R_LOGO_16

class RTemplatesFactory : ProjectTemplatesFactory() {

  override fun getGroups() = arrayOf(RBundle.message("module.group.name"))
  override fun getGroupIcon(group: String) = R_LOGO_16

  override fun createTemplates(group: String?, context: WizardContext?): Array<out ProjectTemplate> {
    return arrayOf<ProjectTemplate>(REmptyProjectGenerator(), RPackageProjectGenerator())
  }
}
