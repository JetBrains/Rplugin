package org.jetbrains.r.packages

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.systemIndependentPath
import com.intellij.util.ui.FilePathSplittingPolicy
import org.jetbrains.r.interpreter.RInterpreterBaseTestCase
import org.jetbrains.r.interpreter.RInterpreterLocation
import org.jetbrains.r.interpreter.RInterpreterState
import org.jetbrains.r.interpreter.RInterpreterStateManager
import java.nio.file.Files
import java.nio.file.Path

class RIsolatedPackageEnvironment : RInterpreterBaseTestCase() {

  fun testSymlinkPackages() {
    val state = RInterpreterStateManager.getCurrentStateBlocking(myFixture.project, DEFAULT_TIMEOUT)!!
    val interpreterLocation = state.rInterop.interpreter.interpreterLocation
    val installedPackage = state.installedPackages.map { it.name to Path.of(it.libraryPath, it.name) }.toMap()
    val skeletonPaths = getSkeletonPaths(state, interpreterLocation)

    rInterop.executeCode("""
      binding_replace <- function(symbol, replacement) {
        base <- .BaseNamespaceEnv
        base${'$'}unlockBinding(symbol, base)
        assign(symbol, replacement, envir = base)
        base${'$'}lockBinding(symbol, base)
      }
    """.trimIndent())

    // Make .Library.site empty
    rInterop.executeCode("binding_replace('.Library.site', '')")
    // Make .Library empty
    rInterop.executeCode("binding_replace('.Library', '')")

    val libPath = Path.of(myFixture.project.basePath, "library")
    Files.createDirectories(libPath)
    for (packageName in packageNamesForTests) {
      Files.createSymbolicLink(libPath.resolve(packageName), installedPackage.getValue(packageName))
    }
    // Rewrite .libPaths
    rInterop.executeCode(".libPaths('${libPath.systemIndependentPath}')")
    rInterop.updateState().blockingGet(DEFAULT_TIMEOUT)
    val newSkeletonPaths = getSkeletonPaths(state, interpreterLocation)

    assertFalse(skeletonPaths.keys.toSet() == newSkeletonPaths.keys.toSet()) // Just in case
    assertEquals(packageNamesForTests, newSkeletonPaths.keys.toSet())
    for (key in newSkeletonPaths.keys) {
      assertEquals(newSkeletonPaths.getValue(key), skeletonPaths.getValue(key))
    }
  }

  private fun getSkeletonPaths(state: RInterpreterState, interpreterLocation: RInterpreterLocation): Map<String, Path> {
    return state.installedPackages.map {
      it.name to RSkeletonUtil.installedPackageToSkeletonPath(state.skeletonsDirectory, it, interpreterLocation)
    }.toMap()
  }

  companion object {
    private val packageNamesForTests: Set<String> = """
      base
      datasets
      data.table
      dplyr
      graphics
      grDevices
      stats
      utils
      roxygen2
      ggplot2
      tibble
    """.trimIndent().split("\n").toSet()
  }
}