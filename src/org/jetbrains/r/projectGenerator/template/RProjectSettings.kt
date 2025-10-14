/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.template

import com.intellij.r.psi.interpreter.RInterpreterLocation

class RProjectSettings {
  var useNewInterpreter: Boolean = true
  var interpreterLocation: RInterpreterLocation? = null
  var installedPackages: Set<String> = emptySet()
  var isInstalledPackagesSetUpToDate: Boolean = false
}