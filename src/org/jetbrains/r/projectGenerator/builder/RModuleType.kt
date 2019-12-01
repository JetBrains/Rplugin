/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.builder

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.R_LOGO_16
import javax.swing.Icon

open class RModuleType : ModuleType<ModuleBuilder>(R_MODULE) {

  /**
   * Always return default module builder. It is impossible to return [RModuleBuilder]
   * without a [RProjectGenerator][org.jetbrains.r.projectGenerator.template.RProjectGenerator]
   */
  override fun createModuleBuilder(): ModuleBuilder = ModuleTypeManager.getInstance().defaultModuleType.createModuleBuilder()

  override fun getName(): String = RBundle.message("module.type.name")

  override fun getDescription(): String = RBundle.message("module.type.description")

  override fun getNodeIcon(isOpened: Boolean): Icon = R_LOGO_16!!

  companion object {
    const val R_MODULE = "R_MODULE"

    val instance: ModuleType<*>
      get() = ModuleTypeManager.getInstance().findByID(R_MODULE)
  }
}