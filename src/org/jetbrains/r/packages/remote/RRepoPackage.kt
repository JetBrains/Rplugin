/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote

import com.intellij.webcore.packaging.RepoPackage

class RRepoPackage(
  name: String,
  repoUrl: String,
  latestVersion: String,
  val depends: String?
) : RepoPackage(name, repoUrl, latestVersion) {

  override fun toString(): String {
    return "RRepoPackage{name=$name, repoUrl=$repoUrl, latestVersion=$latestVersion, depends=$depends}"
  }
}
