package org.jetbrains.r.psi.references

import com.intellij.pom.PomDescriptionProvider
import com.intellij.pom.PomTarget
import com.intellij.psi.ElementDescriptionLocation
import com.intellij.usageView.UsageViewTypeLocation
import org.jetbrains.r.RBundle
import org.jetbrains.r.psi.RSkeletonParameterPomTarget

class RPomTargetDescriptionProvider : PomDescriptionProvider() {
  override fun getElementDescription(element: PomTarget, location: ElementDescriptionLocation): String?{
    if (element !is RSkeletonParameterPomTarget) return null
    return when (location) {
      UsageViewTypeLocation.INSTANCE -> RBundle.message("find.usages.parameter")
      else -> element.parameterName
    }
  }
}