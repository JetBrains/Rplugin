/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

enum class ExportGlobalEnvPolicy {
  DO_NO_EXPORT {
    override fun toString() = "Do not copy results"
  },
  EXPORT_TO_GLOBAL_ENV {
    override fun toString() = "Copy results to global environment"
  },
  EXPORT_TO_VARIABLE {
    override fun toString() = "Copy results to variable"
  }
}

data class RJobTask(val scriptPath: String,
                    val workingDirectory: String,
                    val importGlobalEnv: Boolean,
                    val exportGlobalEnv: ExportGlobalEnvPolicy)