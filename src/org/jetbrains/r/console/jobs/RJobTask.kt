/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.r.psi.RBundle

enum class ExportGlobalEnvPolicy {
  DO_NO_EXPORT {
    override fun toString() = RBundle.message("jobs.export.do.no.copy")
  },
  EXPORT_TO_GLOBAL_ENV {
    override fun toString() = RBundle.message("jobs.export.copy.to.global.env")
  },
  EXPORT_TO_VARIABLE {
    override fun toString() = RBundle.message("jobs.export.copy.to.variable")
  }
}

data class RJobTask(val script: VirtualFile,
                    val workingDirectory: String,
                    val importGlobalEnv: Boolean,
                    val exportGlobalEnv: ExportGlobalEnvPolicy)