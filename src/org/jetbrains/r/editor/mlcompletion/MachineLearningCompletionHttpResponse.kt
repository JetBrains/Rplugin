package org.jetbrains.r.editor.mlcompletion

import org.jetbrains.io.mandatory.Mandatory
import org.jetbrains.io.mandatory.RestModel

@RestModel
data class MachineLearningCompletionHttpResponse(@Mandatory val completionVariants: List<CompletionVariant>) {
  @RestModel
  data class CompletionVariant(@Mandatory val text: String, @Mandatory val score: Double)
}
