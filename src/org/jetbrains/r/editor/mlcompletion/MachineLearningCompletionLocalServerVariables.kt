package org.jetbrains.r.editor.mlcompletion


object MachineLearningCompletionLocalServerVariables {
  private const val NUM_THREADS = 8

  private val THREAD_VARIABLES =
    listOf("OMP_NUM_THREADS",
           "OPENBLAS_NUM_THREADS",
           "MKL_NUM_THREADS",
           "VECLIB_MAXIMUM_THREADS",
           "NUMEXPR_NUM_THREADS")

  val SERVER_ENVIRONMENT: Map<String, String> =
    THREAD_VARIABLES.associateBy({ it }, { NUM_THREADS.toString() })
}
