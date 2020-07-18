/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Version

abstract class RInterpreterBase(private val versionInfo: Map<String, String>,
                                override val project: Project) : RInterpreter {

  override val version: Version = buildVersion(versionInfo)
  override val interpreterName: String get() = versionInfo["version.string"]?.replace(' ', '_')  ?: "unnamed"

  open fun onSetAsProjectInterpreter() {
    RLibraryWatcher.getInstance(project).setCurrentInterpreter(this)
  }

  open fun onUnsetAsProjectInterpreter() {
    RLibraryWatcher.getInstance(project).setCurrentInterpreter(null)
  }

  private fun buildVersion(versionInfo: Map<String, String>): Version {
    val major = versionInfo["major"]?.toInt() ?: 0
    val minorAndUpdate = versionInfo["minor"]?.split(".")
    val minor = if (minorAndUpdate?.size == 2) minorAndUpdate[0].toInt() else 0
    val update = if (minorAndUpdate?.size == 2) minorAndUpdate[1].toInt() else 0
    return Version(major, minor, update)
  }

  companion object {
    val LOG = Logger.getInstance(RInterpreterBase::class.java)
  }
}