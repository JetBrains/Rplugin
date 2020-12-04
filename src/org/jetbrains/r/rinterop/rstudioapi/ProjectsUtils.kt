package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.rStudioProjects.RStudioProjectSettings
import org.jetbrains.r.rStudioProjects.writeRProjectFile
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RObject
import java.io.File
import java.nio.file.Path

object ProjectsUtils {
  fun getActiveProject(rInterop: RInterop): RObject {
    val path = rInterop.interpreter.basePath
    return path.toRString()
  }

  fun openProject(args: RObject) {
    val rProjFile = args.list.getRObjects(0).rString.getStrings(0)
    val newSession = args.list.getRObjects(1).rBoolean.getBooleans(0)
    val baseDir = PathUtil.getParentPath(rProjFile)
    val options = OpenProjectTask(forceOpenInNewFrame = newSession,
                                  projectName = FileUtil.getNameWithoutExtension(PathUtil.getFileName(rProjFile)),
                                  runConfigurators = true
    )
    ProjectManagerEx.getInstanceEx().openProject(Path.of(baseDir), options)
    return
  }

  fun writeProjectFile(args: RObject): Promise<RObject> {
    val rProjFile = args.list.getRObjects(0).rString.getStrings(0)
    val promise = AsyncPromise<RObject>()
    runWriteAction {
      try {
        writeRProjectFile(RStudioProjectSettings.DEFAULT_INSTANCE, File(rProjFile))
        promise.setResult(true.toRBoolean())
      }
      catch (e: Exception) {
        promise.setError(e)
      }
    }
    return promise
  }
}