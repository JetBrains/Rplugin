package org.jetbrains.r.packages

import com.intellij.r.psi.interpreter.RInterpreterState
import com.intellij.r.psi.interpreter.RInterpreterStateManager
import org.jetbrains.r.interpreter.RInterpreterBaseTestCase
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString

class RIsolatedPackageEnvironment : RInterpreterBaseTestCase() {

  fun testSymlinkPackages() {
    val state = RInterpreterStateManager.getCurrentStateBlocking(myFixture.project, DEFAULT_TIMEOUT)!!
    val installedPackage = state.installedPackages.map { it.name to Path.of(it.libraryPath, it.name) }.toMap()
    val skeletonPaths = getSkeletonPaths(state)

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
    rInterop.executeCode(".libPaths('${libPath.invariantSeparatorsPathString}')")
    rInterop.updateState().blockingGet(DEFAULT_TIMEOUT)
    val newSkeletonPaths = getSkeletonPaths(state)

    assertFalse(skeletonPaths.keys.toSet() == newSkeletonPaths.keys.toSet()) // Just in case
    assertEquals(packageNamesForTests, newSkeletonPaths.keys.toSet())
    for (key in newSkeletonPaths.keys) {
      assertEquals(newSkeletonPaths.getValue(key), skeletonPaths.getValue(key))
    }
  }

  private fun getSkeletonPaths(state: RInterpreterState): Map<String, Path> {
    return state.installedPackages.map {
      it.name to RSkeletonUtil.installedPackageToSkeletonPath(state.skeletonsDirectory, it)
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