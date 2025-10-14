package org.jetbrains.r.run.visualize

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.notifications.RNotificationUtil
import com.intellij.r.psi.rinterop.RInterop
import com.intellij.r.psi.rinterop.RInteropTerminated
import com.intellij.r.psi.rinterop.RReference
import com.intellij.r.psi.run.visualize.RVisualization
import org.jetbrains.concurrency.await
import org.jetbrains.r.packages.RequiredPackageException
import org.jetbrains.r.packages.RequiredPackageInstaller
import org.jetbrains.r.rinterop.RInteropImpl

class RVisualizationImpl(val project: Project) : RVisualization {
  override suspend fun visualizeTable(rInterop: RInterop, ref: RReference, expr: String, editor: Editor?) {
    val viewer = try {
      rInterop as RInteropImpl
      rInterop.dataFrameGetViewer(ref).await()
    }
    catch (exception: RDataFrameException) {
      notifyError(project, editor, RBundle.message("visualize.table.action.error.hint", exception.message.orEmpty()))
      logger<VisualizeTableAction>().warn(exception)
      null
    }
    catch (exception: RInteropTerminated) {
      notifyError(project, editor, RBundle.message("rinterop.terminated"))
      logger<VisualizeTableAction>().warn(exception)
      null
    }
    catch (exception: RequiredPackageException) {
      RequiredPackageInstaller.getInstance(project).installPackagesWithUserPermission(RBundle.message("visualize.table.action.error.utility.name"), exception.missingPackages)
      logger<VisualizeTableAction>().warn(exception)
      null
    }

    if (viewer != null) {
      RVisualizeTableUtil.showTableAsync(project, viewer, expr)
    }
  }

  private fun notifyError(project: Project, editor: Editor?, @NlsSafe errorMsg: String) {
    if (editor != null) HintManager.getInstance().showErrorHint(editor, errorMsg)
    else RNotificationUtil.notifyExecutionError(project, errorMsg)
  }
}
