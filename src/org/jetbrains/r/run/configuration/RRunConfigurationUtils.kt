// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.configuration

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.r.RFileType
import java.io.File

object RRunConfigurationUtils {
  fun checkConfiguration(runConfiguration: RRunConfiguration) {
    if (StringUtil.isEmptyOrSpaces(runConfiguration.scriptPath)) {
      throw ConfigurationException("There is unspecified parameter in R run configuration: script")
    }
  }

  fun suggestedName(configuration: RRunConfiguration): String? {
    val scriptPath = configuration.scriptPath
    if (StringUtil.isEmptyOrSpaces(scriptPath)) return null
    val name = File(scriptPath).name
    val dotAndExtension = "." + RFileType.defaultExtension
    return if (name.length > dotAndExtension.length && StringUtil.endsWithIgnoreCase(name, dotAndExtension)) {
      name.substring(0, name.length - dotAndExtension.length)
    }
    else name
  }
}
