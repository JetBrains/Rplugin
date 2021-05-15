package org.jetbrains.r.classes.s4.classInfo

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.ManipulatableTarget
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.classes.s4.RS4SourceManager
import org.jetbrains.r.psi.RPomTarget
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RExpression
import org.jetbrains.r.psi.api.RStringLiteralExpression
import org.jetbrains.r.skeleton.psi.RSkeletonCallExpression

data class RSkeletonS4SlotPomTarget(val setClass: RSkeletonCallExpression, private val name: String) : RPomTarget() {
  override fun navigateAsync(requestFocus: Boolean): Promise<Unit> {
    return runAsync {
      runReadAction {
        val rCall = RS4SourceManager.getCallFromSkeleton(setClass)
        val slots = (rCall.argumentList.namedArgumentList.first { it.name == "slots" }.assignedValue as RCallExpression).argumentList
        val reqSlot = slots.namedArgumentList.first { it.name == name }
        invokeLater { reqSlot.navigate(true) }
      }
    }
  }
  override fun getName(): String = name

  val slotInfo by lazy { setClass.associatedS4ClassInfo.slots.firstOrNull { it.name == name } }
}

data class RS4ComplexSlotPomTarget(val slotDefinition: RExpression, val slot: RS4ClassSlot) : RPomTarget() {
  override fun navigateAsync(requestFocus: Boolean): Promise<Unit> {
    return runAsync { invokeLater { slotDefinition.navigate(true) } }
  }
  override fun getName(): String = slot.name
}

data class RStringLiteralPomTarget(val literal: RStringLiteralExpression) : ManipulatableTarget(literal)

data class RSkeletonS4ClassPomTarget(val setClass: RSkeletonCallExpression) : RPomTarget() {
  override fun navigateAsync(requestFocus: Boolean): Promise<Unit> {
    return runAsync {
      runReadAction {
        val rCall = RS4SourceManager.getCallFromSkeleton(setClass)
        val className = rCall.argumentList.expressionList.first() as RStringLiteralExpression
        invokeLater { RStringLiteralPomTarget(className).navigate(true) }
      }
    }
  }
  override fun getName(): String = setClass.stub.s4ClassInfo.className
}
