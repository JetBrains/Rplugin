package com.intellij.r.psi.psi.references

import com.intellij.pom.PomDescriptionProvider
import com.intellij.pom.PomTarget
import com.intellij.psi.ElementDescriptionLocation
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.psi.RSkeletonParameterPomTarget
import com.intellij.usageView.UsageViewTypeLocation

class RPomTargetDescriptionProvider : PomDescriptionProvider() {
  override fun getElementDescription(element: PomTarget, location: ElementDescriptionLocation): String?{
    if (element !is RSkeletonParameterPomTarget) return null
    return when (location) {
      UsageViewTypeLocation.INSTANCE -> RBundle.message("find.usages.parameter")
      else -> element.parameterName
    }
  }
}