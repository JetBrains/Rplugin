package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.util.PathUtil
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.asPromise
import org.jetbrains.r.console.jobs.ExportGlobalEnvPolicy
import org.jetbrains.r.console.jobs.RJobRunner
import org.jetbrains.r.console.jobs.RJobTask
import org.jetbrains.r.console.jobs.RJobsToolWindowFactory
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RObject
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.findFileByPathAtHostHelper
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.getRNull
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.rError
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.toRString
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.toStringOrNull

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
        val runner = RJobRunner.getInstance(rInterop.project)
        runner.coroutineScope.async(ModalityState.defaultModalityState().asContextElement()) {
          val descriptor = runner.runRJob(RJobTask(it, workingDir, importEnv, exportEnv), exportEnvName, name)
          "${descriptor.hashCode()}".toRString()
        }.asCompletableFuture().asPromise().processed(promise)
      }
      else {
        promise.setResult(getRNull())
      }
    }
    return promise
  }

  fun jobRemove(rInterop: RInterop, args: RObject): RObject {
    val id = args.rString.getStrings(0).toInt()
    val jobList = RJobsToolWindowFactory.getJobsPanel(rInterop.project)?.jobList ?: return getRNull()
    jobList.removeJobEntity(jobList.jobEntities.find {
      it.jobDescriptor.hashCode() == id
    } ?: return rError("Job ID '${id}' does not exist."))
    return getRNull()
  }
}