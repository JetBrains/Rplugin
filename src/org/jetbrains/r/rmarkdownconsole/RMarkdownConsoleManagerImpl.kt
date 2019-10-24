/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdownconsole

import com.intellij.openapi.project.Project

class RMarkdownConsoleManagerImpl(private val project: Project) : RMarkdownConsoleManager {

  override val consoleRunner: RMarkdownConsoleRunner by lazy { initializeRMarkdownConsoleRunner() }

  private fun initializeRMarkdownConsoleRunner(): RMarkdownConsoleRunner{
    return RMarkdownConsoleRunner(project, project.basePath!!)
  }
}
