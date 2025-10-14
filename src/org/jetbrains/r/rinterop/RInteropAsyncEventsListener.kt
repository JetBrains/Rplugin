package org.jetbrains.r.rinterop

import com.intellij.execution.process.ProcessOutputType
import com.intellij.r.psi.debugger.RSourcePosition
import com.intellij.r.psi.rinterop.RInterop
import com.intellij.r.psi.rinterop.RObject
import com.intellij.r.psi.rinterop.RReference
import com.intellij.r.psi.rinterop.RValue
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiFunctionId
import org.jetbrains.r.run.visualize.RDataFrameViewer

internal interface RInteropAsyncEventsListener {
  fun onText(text: String, type: ProcessOutputType) {}
  fun onBusy() {}
  fun onRequestReadLn(prompt: String) {}
  fun onPrompt(isDebug: Boolean = false) {}
  fun onException(exception: RExceptionInfo) {}
  fun onTermination() {}
  suspend fun onViewRequest(ref: RReference, title: String, value: RValue) {}
  fun onViewTableRequest(viewer: RDataFrameViewer, title: String) {}
  fun onShowHelpRequest(httpdResponse: RInterop.HttpdResponse) {}
  suspend fun onShowFileRequest(filePath: String, title: String) {}
  suspend fun onRStudioApiRequest(functionId: RStudioApiFunctionId, args: RObject): RObject? = null
  fun onSubprocessInput() {}
  fun onBrowseURLRequest(url: String) {}
  fun onRemoveBreakpointByIdRequest(id: Int) {}
  fun onDebugPrintSourcePositionRequest(position: RSourcePosition) {}
}