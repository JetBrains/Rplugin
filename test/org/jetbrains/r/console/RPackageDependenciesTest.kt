/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.packages.RPackageProjectManager

class RPackageDependenciesTest : RConsoleBaseTestCase() {

  fun testLoadDependencies() {
    addLibraries()
    val packages = RInterpreterManager.getInterpreter(project)?.installedPackages ?: error("No interpreter")
    val packageMap = packages.map { it.packageName to it.packageVersion }.toMap()
    val dplyrVersion = packageMap["dplyr"] ?: error("Dplyr no found")
    val ggplot2Version = packageMap["ggplot2"] ?: error("Dplyr no found")
    myFixture.addFileToProject("DESCRIPTION", """
      Package: testPackage
      Title: Test package
      Version: 1.0
      Date: 2020-03-09
      Author: Who wrote it
      Description: Test package
      License: GPL (>= 2)
      Depends: R
      Imports: dplyr (>= $dplyrVersion)
      Suggests:
        ggplot2 (== $ggplot2Version),
        somePackage (> 10.0.1)
    """.trimIndent())

    val beforeRefresh = getLoadedPackages()
    assertDoesntContain(beforeRefresh, "dplyr", "ggplot2", "R", "somePackage")

    RPackageProjectManager.getInstance(myFixture.project).getProjectPackageDescriptionInfo()
    val afterRefresh = getLoadedPackages()
    assertDoesntContain(afterRefresh, "R", "somePackage")
    UsefulTestCase.assertContainsElements(afterRefresh, "dplyr", "ggplot2")
  }

  private fun getLoadedPackages(): Set<String> {
    return rInterop.executeCode("cat(loadedNamespaces())").stdout.split(" ").toSet()
  }
}