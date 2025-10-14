package com.intellij.r.psi.packages

import com.intellij.openapi.project.Project

data class DependencyVersionBound(
  val version: String,
  val strict: Boolean = false
)

data class DependencyPackage(
  val name: String,
  val lowerBound: DependencyVersionBound? = null,
  val upperBound: DependencyVersionBound? = null
) {
  fun versionSatisfies(version: String): Boolean {
    return lowerBoundSatisfies(version) && upperBoundSatisfies(version)
  }

  fun lowerBoundSatisfies(version: String): Boolean {
    if (lowerBound == null) return true
    val compareResult = RPackageVersion.compare(version, lowerBound.version) ?: return true
    return compareResult > 0 || !lowerBound.strict && compareResult == 0
  }

  private fun upperBoundSatisfies(version: String): Boolean {
    if (upperBound == null) return true
    val compareResult = RPackageVersion.compare(version, upperBound.version) ?: return true
    return compareResult < 0 || !upperBound.strict && compareResult == 0
  }
}

data class PackageDescriptionInfo(
  val packageName: String,
  val version: String,
  val title: String,
  val depends: List<DependencyPackage>,
  val imports: List<DependencyPackage>,
  val suggests: List<DependencyPackage>
)

interface RPackageProjectManager {
  /**
   * @return *null* if no DESCRIPTION file or is it written **pretty** bad (mandatory fields are omitted,
   * gross syntax errors, etc.)
   */
  fun getProjectPackageDescriptionInfo(): PackageDescriptionInfo?

  fun loadOrSuggestToInstallMissedPackages(packageInfo: PackageDescriptionInfo? = getProjectPackageDescriptionInfo())

  companion object {
    fun getInstance(project: Project): RPackageProjectManager {
      return project.getService(RPackageProjectManager::class.java)
    }
  }
}
