package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.r.psi.rinterop.RObject
import com.intellij.util.PathUtil
import org.jetbrains.concurrency.await
import org.jetbrains.r.console.jobs.ExportGlobalEnvPolicy
import org.jetbrains.r.console.jobs.RJobRunner
import org.jetbrains.r.console.jobs.RJobTask
import org.jetbrains.r.console.jobs.RJobsToolWindowFactory
import org.jetbrains.r.rinterop.RInteropImpl
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.findFileByPathAtHostHelper
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.getRNull
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.rError
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.toRString
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.toStringOrNull

object JobUtils {
  suspend fun jobRunScript(rInterop: RInteropImpl, args: RObject): RObject {
    val path = args.list.getRObjects(0).rString.getStrings(0)
    val name = args.list.getRObjects(1).toStringOrNull()
    val file = findFileByPathAtHostHelper(rInterop, path).await()
    if (file == null) return getRNull()

    val workingDir = args.list.getRObjects(3).toStringOrNull() ?: PathUtil.getParentPath(path)
    val importEnv = args.list.getRObjects(4).rBoolean.getBooleans(0)
    val exportEnvName = args.list.getRObjects(5).rString.getStrings(0)
    val exportEnv = when (exportEnvName) {
      "" -> ExportGlobalEnvPolicy.DO_NO_EXPORT
      "R_GlobalEnv" -> ExportGlobalEnvPolicy.EXPORT_TO_GLOBAL_ENV
      else -> ExportGlobalEnvPolicy.EXPORT_TO_VARIABLE
    }

    val runner = RJobRunner.getInstance(rInterop.project)
    val descriptor = runner.runRJob(RJobTask(file, workingDir, importEnv, exportEnv), exportEnvName, name)
    return "${descriptor.hashCode()}".toRString()
  }

  fun jobRemove(rInterop: RInteropImpl, args: RObject): RObject {
    val id = args.rString.getStrings(0).toInt()
    val jobList = RJobsToolWindowFactory.getJobsPanel(rInterop.project)?.jobList ?: return getRNull()
    jobList.removeJobEntity(jobList.jobEntities.find {
      it.jobDescriptor.hashCode() == id
    } ?: return rError("Job ID '${id}' does not exist."))
    return getRNull()
  }
}
