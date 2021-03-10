package org.jetbrains.r.editor.mlcompletion

data class MachineLearningCompletionHttpResponse(val completionVariants: List<CompletionVariant>) {
  data class CompletionVariant(val text: String, val score: Double)
}
