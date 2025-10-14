package com.intellij.r.psi.classes.r6

class R6ClassKeywordsProvider {
  companion object {
    val predefinedClassMethods: List<R6ClassMethod> = listOf(
      R6ClassMethod("clone", "", true)
    )

    val visibilityModifiers: List<String> = listOf(
      R6ClassInfoUtil.argumentPrivate,
      R6ClassInfoUtil.argumentPublic,
      R6ClassInfoUtil.argumentActive,
    )
  }
}