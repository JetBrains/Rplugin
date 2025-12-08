package com.intellij.r.psi.run.graphics

sealed class RGraphicsUpdate

data class RGraphicsLoadingUpdate(val loadedCount: Int, val totalCount: Int): RGraphicsUpdate()

data class RGraphicsCompletedUpdate(val outputs: List<RGraphicsOutput>): RGraphicsUpdate()
