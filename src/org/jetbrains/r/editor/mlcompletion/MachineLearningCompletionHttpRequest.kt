package org.jetbrains.r.editor.mlcompletion

data class MachineLearningCompletionHttpRequest(
  val isInsideToken: Boolean,
  val text: CharSequence
)
