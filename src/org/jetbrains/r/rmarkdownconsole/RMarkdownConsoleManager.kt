/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdownconsole

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project

interface RMarkdownConsoleManager{
  val consoleRunner: RMarkdownConsoleRunner

  companion object {
    fun getInstance(project: Project): RMarkdownConsoleManager = ServiceManager.getService(project, RMarkdownConsoleManager::class.java)
  }
}
