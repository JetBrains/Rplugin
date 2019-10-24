/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdownconsole


import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType.ERROR_OUTPUT
import com.intellij.openapi.project.Project

class RMarkdownConsoleView(project: Project) : ConsoleViewImpl(project, false) {

  fun printNotFoundMessage(){
    this.print("\nRMarkdown file was not found\n", ERROR_OUTPUT)
  }

  fun printResultNotFoundMessage(){
    this.print("\nResult file was not found\n", ERROR_OUTPUT)
  }

  fun printRenderingInterruptedMessage(){
    this.print("\nRendering was interrupted\n", ERROR_OUTPUT)
  }
}
