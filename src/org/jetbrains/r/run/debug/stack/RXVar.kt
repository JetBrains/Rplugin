/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package icons.org.jetbrains.r.run.debug.stack

import com.intellij.xdebugger.frame.*
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.debugger.exception.RDebuggerException
import org.jetbrains.r.rinterop.RValueEnvironment
import org.jetbrains.r.rinterop.RValueSimple
import org.jetbrains.r.rinterop.RVar
import org.jetbrains.r.run.debug.stack.RXPresentationUtils
import org.jetbrains.r.run.debug.stack.addEnvironmentContents
import java.util.concurrent.ExecutorService
import kotlin.math.min

internal class RXVar internal constructor(private val rVar: RVar, private val executor: ExecutorService) : XNamedValue(rVar.name) {
  private var offset = 0
  private val loader by lazy { rVar.ref.createVariableLoader() }

  override fun computePresentation(node: XValueNode, place: XValuePlace) {
    RXPresentationUtils.setPresentation(rVar, node, executor)
  }

  override fun computeChildren(node: XCompositeNode) {
    executor.execute {
      try {
        val result = XValueChildrenList()
        val endOffset = offset + MAX_ITEMS
        val (vars, totalCount) = loader.loadVariablesPartially(offset, endOffset)
        if (rVar.value is RValueEnvironment) {
          addEnvironmentContents(result, vars, executor)
        } else {
          addListContents(result, vars, executor, rVar.value is RValueSimple, offset)
        }
        node.addChildren(result, true)
        offset = min(endOffset, totalCount)
        if (offset != totalCount) {
          node.tooManyChildren(totalCount - offset)
        }
      } catch (e: RDebuggerException) {
        node.setErrorMessage(e.message.orEmpty())
      }
    }
  }

  companion object {
    private const val MAX_ITEMS = 100
  }
}

private fun addListContents(result: XValueChildrenList, vars: List<RVar>, executor: ExecutorService,
                                    isVector: Boolean = false, offset: Int = 0) {
  vars.forEachIndexed { index, it ->
    if (it.name.isEmpty()) {
      val message = if (isVector) "rx.presentation.utils.vector.element.name" else "rx.presentation.utils.list.element.name"
      result.add(RXVar(it.copy(name = RBundle.message(message, offset + index + 1)), executor))
    } else {
      result.add(RXVar(it, executor))
    }
  }
}
