package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.r.psi.RBundle
import com.intellij.util.containers.toArray
import org.jetbrains.r.inspections.RInspection
import java.util.concurrent.ConcurrentHashMap

class RStudioAPISourceMarkerInspection : RInspection() {

  override fun getDisplayName(): String {
    return RBundle.message("rstudioapi.source.markers.name")
  }

  override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
    val markers = file.project.getUserData(SOURCE_MARKERS_KEY)?.get(file.virtualFile?.path ?: return emptyArray()) ?: return emptyArray()
    val problemDescriptors = mutableListOf<ProblemDescriptor>()
    markers.toList().map { (_, problems) ->
      val document = file.virtualFile?.let { FileDocumentManager.getInstance().getDocument(it) }
      if (document != null) {
        problems.map { problem ->
          val offset = RStudioApiUtils.getLineOffset(document, problem.line - 1, problem.col) - 1
          val problemDescriptor = manager.createProblemDescriptor(
            file,
            TextRange(offset, offset), problem.message,
            problem.type, false
          )
          problemDescriptors.add(problemDescriptor)
        }
      }
    }
    return problemDescriptors.toArray(emptyArray())
  }

  companion object {
    internal val SOURCE_MARKERS_KEY = Key<ConcurrentHashMap<String, SourceMarkers>>("org.jetbrains.r.rinterop.rstudioapi.source.markers")
  }

  internal data class RStudioAPIMarker(val type: ProblemHighlightType, val message: String, val line: Int, val col: Int)
}

internal typealias SourceMarkers = ConcurrentHashMap<String, List<RStudioAPISourceMarkerInspection.RStudioAPIMarker>>