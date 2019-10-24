/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.template

class RProjectSettings {
  var useNewInterpreter: Boolean = true
  var interpreterPath: String? = null
  var rScriptPath: String? = null
  var installedPackages: Set<String> = emptySet()
  var isInstalledPackagesSetUpToDate: Boolean = false
}