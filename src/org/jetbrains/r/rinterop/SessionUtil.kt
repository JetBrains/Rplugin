/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.r.interpreter.RInterpreterManager
import java.nio.file.Paths

object SessionUtil {
  fun getWorkspaceFile(project: Project): String? {
    val filename: String
    val projectDir: VirtualFile
    if (ApplicationManager.getApplication().isUnitTestMode) {
      if (project.getUserData(ENABLE_SAVE_SESSION_IN_TESTS) == null) return null
      projectDir = PlatformTestUtil.getOrCreateProjectTestBaseDir(project)
      filename = "a"
    } else {
      projectDir = project.guessProjectDir() ?: return null
      val interpreter = RInterpreterManager.getInterpreter(project) ?: return null
      filename = interpreter.interpreterPath.hashCode().toString()
    }
    return Paths.get(projectDir.canonicalPath ?: return null, WORKSPACE_DIRECTORY, "$filename.$EXTENSION").toString()
  }

  internal val ENABLE_SAVE_SESSION_IN_TESTS = Key<Unit>("org.jetbrains.r.rinterop.EnableSaveSessionInTests")
  private const val WORKSPACE_DIRECTORY = ".RDataFiles"
  private const val EXTENSION = "RData"
}