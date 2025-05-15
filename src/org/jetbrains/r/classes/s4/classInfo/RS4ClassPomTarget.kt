/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.s4.classInfo

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.psi.ManipulatableTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.r.classes.s4.RS4SourceManager
import org.jetbrains.r.psi.RPomTarget
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RExpression
import org.jetbrains.r.psi.api.RStringLiteralExpression
import org.jetbrains.r.skeleton.psi.RSkeletonCallExpression

data class RSkeletonS4SlotPomTarget(val setClass: RSkeletonCallExpression, private val name: String) : RPomTarget() {
  override suspend fun navigateAsync(requestFocus: Boolean) {
    val reqSlot = readAction {
      val rCall = RS4SourceManager.getCallFromSkeleton(setClass)
      val slots = (rCall.argumentList.namedArgumentList.first { it.name == "slots" }.assignedValue as RCallExpression).argumentList
      slots.namedArgumentList.first { it.name == name }
    }
    withContext(Dispatchers.EDT) {
      reqSlot.navigate(true)
    }
  }

  override fun getName(): String = name

  val slotInfo by lazy { setClass.associatedS4ClassInfo!!.slots.firstOrNull { it.name == name } }
}

data class RS4ComplexSlotPomTarget(val slotDefinition: RExpression, val slot: RS4ClassSlot) : RPomTarget() {
  override suspend fun navigateAsync(requestFocus: Boolean) {
    withContext(Dispatchers.EDT) {
      slotDefinition.navigate(true)
    }
  }

  override fun getName(): String = slot.name
}

data class RStringLiteralPomTarget(val literal: RStringLiteralExpression) : ManipulatableTarget(literal)

data class RSkeletonS4ClassPomTarget(val setClass: RSkeletonCallExpression) : RPomTarget() {
  override suspend fun navigateAsync(requestFocus: Boolean) {
    val className = readAction {
      val rCall = RS4SourceManager.getCallFromSkeleton(setClass)
      rCall.argumentList.expressionList.first() as RStringLiteralExpression
    }
    withContext(Dispatchers.EDT) {
      RStringLiteralPomTarget(className).navigate(true)
    }
  }

  override fun getName(): String = setClass.stub.s4ClassInfo.className
}
