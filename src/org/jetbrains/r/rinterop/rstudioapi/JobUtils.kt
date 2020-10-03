package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.util.PathUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.console.jobs.ExportGlobalEnvPolicy
import org.jetbrains.r.console.jobs.RJobRunner
import org.jetbrains.r.console.jobs.RJobTask
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RObject

object JobUtils {
  fun jobRunScript(rInterop: RInterop, args: RObject): Promise<RObject> {
    val path = args.list.getRObjects(0).rString.getStrings(0)
    val name = args.list.getRObjects(1).toStringOrNull()
    val filePromise = findFileByPathAtHostHelper(rInterop, path)
    val workingDir = args.list.getRObjects(3).toStringOrNull() ?: PathUtil.getParentPath(path)
    val importEnv = args.list.getRObjects(4).rBoolean.getBooleans(0)
    val exportEnvName = args.list.getRObjects(5).rString.getStrings(0)
    val exportEnv = when (exportEnvName) {
      "" -> ExportGlobalEnvPolicy.DO_NO_EXPORT
      "R_GlobalEnv" -> ExportGlobalEnvPolicy.EXPORT_TO_GLOBAL_ENV
      else -> ExportGlobalEnvPolicy.EXPORT_TO_VARIABLE
    }
    val promise = AsyncPromise<RObject>()
    filePromise.then {
      if (it != null) {
        RJobRunner.getInstance(rInterop.project)
          .runRJob(RJobTask(it, workingDir, importEnv, exportEnv), exportEnvName, name).then {
            promise.setResult("${it.hashCode()}".toRString())
          }
      }
      else {
        promise.setResult(getRNull())
      }
    }
    return promise
  }

  fun jobRemove(rInterop: RInterop, args: RObject): RObject {
    val id = args.rString.getStrings(0).toInt()
    val jobList = RConsoleToolWindowFactory.getJobsPanel(rInterop.project)?.jobList ?: return getRNull()
    jobList.removeJobEntity(jobList.jobEntities.find {
      it.jobDescriptor.hashCode() == id
    } ?: return rError("Job ID '${id}' does not exist."))
    return getRNull()
  }
}