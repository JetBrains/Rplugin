package com.intellij.r.psi.run.visualize

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.r.psi.rinterop.RInterop
import com.intellij.r.psi.rinterop.RReference

interface RVisualization {
  suspend fun visualizeTable(rInterop: RInterop, ref: RReference, expr: String, editor: Editor? = null)

  companion object {
    fun getInstance(project: Project): RVisualization = project.getService(RVisualization::class.java)
  }
}