/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.skeleton

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.packages.LibrarySummary
import org.jetbrains.r.packages.remote.RepoUtils
import java.io.FileInputStream

object RSkeletonFileDecompiler {
  fun decompileSymbol(methodName: String, file: VirtualFile, rInterpreter: RInterpreter): CharSequence {
    val binPackage: LibrarySummary.RLibraryPackage = FileInputStream(file.path).use {
      LibrarySummary.RLibraryPackage.parseFrom(it)
    }
    val rPackage = binPackage.name
    val output = rInterpreter.runHelperWithArgs(RepoUtils.DECOMPILER_SCRIPT, rPackage, methodName)

    check(output.exitCode == 0) {
      "Failed to generate source for '" + rPackage + "'. The error was:\n\n" +
      output.stderr +
      "\n\nIf you think this issue with plugin and not your R installation, please file a ticket"
    }
    return output.stdout
  }

}
