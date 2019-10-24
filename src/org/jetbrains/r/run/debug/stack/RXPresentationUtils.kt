// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug.stack

import com.intellij.icons.AllIcons
import com.intellij.xdebugger.frame.XFullValueEvaluator
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.presentation.XValuePresentation
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.packages.RequiredPackageException
import org.jetbrains.r.packages.RequiredPackageInstaller
import org.jetbrains.r.rinterop.*
import org.jetbrains.r.run.visualize.RDataFrameException
import org.jetbrains.r.run.visualize.RVisualizeTableUtil
import java.util.concurrent.ExecutorService

internal object RXPresentationUtils {
  fun setPresentation(rVar: RVar, node: XValueNode, executor: ExecutorService) {
    when (val value = rVar.value) {
      is RValueUnevaluated -> setPromisePresentation(value, rVar.ref, node, executor)
      is RValueSimple -> setVarPresentation(value, rVar.ref, node, executor)
      is RValueDataFrame -> setDataFramePresentation(value, rVar.name, rVar.ref, node, executor)
      is RValueList -> setListPresentation(value, node)
      is RValueFunction -> setFunctionPresentation(value, node)
      is RValueEnvironment -> setEnvironmentPresentation(node, rValue = value)
      is RValueGraph -> setGraphPresentation(rVar.ref, node, executor)
      is RValueError -> setErrorPresentation(value.text, node)
    }
  }

  fun setEnvironmentPresentation(node: XValueNode, rValue: RValueEnvironment? = null) {
    node.setPresentation(AllIcons.Debugger.Value, object : XValuePresentation() {
      override fun renderValue(renderer: XValueTextRenderer) {
        if (rValue != null) {
          val name = rValue.envName.takeIf { it.isNotEmpty() } ?: RBundle.message("rx.presentation.utils.environment.unnamed")
          renderer.renderValue(name)
        }
      }

      override fun getSeparator() = if (rValue != null) super.getSeparator() else ""
    }, true)
  }

  private fun setPromisePresentation(rValue: RValueUnevaluated, ref: RRef, node: XValueNode, executor: ExecutorService) {
    node.setPresentation(AllIcons.Nodes.Unknown, object : XValuePresentation() {
      override fun renderValue(renderer: XValueTextRenderer) {
        renderer.renderComment(rValue.code)
      }
    }, false)
    node.setFullValueEvaluator(object : XFullValueEvaluator(RBundle.message("rx.presentation.utils.evaluate.link.text")) {
      override fun startEvaluation(callback: XFullValueEvaluationCallback) = executor.execute {
        ref.getValueInfo()
        ref.rInterop.invalidateCaches()
        RConsoleManager.getInstance(ref.rInterop.project).currentConsoleOrNull?.debugger?.refreshVariableView()
        callback.evaluated("")
      }

      override fun isShowValuePopup() = false
    })
  }

  private fun setFunctionPresentation(rValue: RValueFunction, node: XValueNode) {
    // TODO: Separate args in rwrapper
    node.setPresentation(AllIcons.Nodes.Function, null, rValue.code.firstLine(), false)
    node.setFullValueEvaluator(object : XFullValueEvaluator(RBundle.message("rx.presentation.utils.view.code.link.text")) {
      override fun startEvaluation(callback: XFullValueEvaluationCallback) {
        callback.evaluated(rValue.code)
      }
    })
  }

  private fun setDataFramePresentation(rValue: RValueDataFrame, name: String, ref: RRef, node: XValueNode, executor: ExecutorService) {
    node.setPresentation(AllIcons.Nodes.DataTables, null,
                         RBundle.message("rx.presentation.utils.data.frame.text", rValue.rows, rValue.cols), true)
    node.setFullValueEvaluator(object : XFullValueEvaluator(RBundle.message("rx.presentation.utils.view.table.link.text")) {
      override fun startEvaluation(callback: XFullValueEvaluationCallback) {
        executor.execute {
          try {
            val viewer = ref.rInterop.dataFrameGetViewer(ref)
            RVisualizeTableUtil.showTable(ref.rInterop.project, viewer, name)
          } catch (e: RDataFrameException) {
            callback.errorOccurred(e.message.orEmpty())
          } catch (e: RequiredPackageException) {
            RequiredPackageInstaller.getInstance(ref.rInterop.project)
              .installPackagesWithUserPermission(RBundle.message("rx.presentation.utils.view.table.utility.name"), e.missingPackages, null)
          } finally {
            callback.evaluated("")
          }
        }
      }

      override fun isShowValuePopup() = false
    })
  }

  private fun setListPresentation(value: RValueList, node: XValueNode) {
    node.setPresentation(AllIcons.Debugger.Db_array, null, RBundle.message("rx.presentation.utils.list.text", value.length), true)
  }

  private fun setVarPresentation(rValue: RValueSimple, rRef: RRef, node: XValueNode, executor: ExecutorService) {
    var line = rValue.text.firstLine()
    if (!rValue.isComplete && rValue.text == line) {
      line += " ..."
    }
    if (line.startsWith("[1]")) {
      line = line.drop(3).trimStart()
    }
    node.setPresentation(AllIcons.Debugger.Db_primitive, object : XValuePresentation() {
      override fun renderValue(renderer: XValueTextRenderer) {
        renderer.renderValue(line)
      }

      override fun getSeparator() = if (line.isNotEmpty()) super.getSeparator() else ""
    }, rValue.isVector)
    if (rValue.text.contains('\n') || !rValue.isComplete) {
      node.setFullValueEvaluator(object : XFullValueEvaluator() {
        override fun startEvaluation(callback: XFullValueEvaluationCallback) {
          if (rValue.isComplete) {
            callback.evaluated(rValue.text)
          } else {
            executor.execute {
              callback.evaluated(rRef.evaluateAsText())
            }
          }
        }
      })
    }
  }

  private fun setGraphPresentation(ref: RRef, node: XValueNode, executor: ExecutorService) {
    node.setPresentation(AllIcons.Nodes.PpLib, null, RBundle.message("rx.presentation.utils.graph.text"), true)
    node.setFullValueEvaluator(object : XFullValueEvaluator(RBundle.message("rx.presentation.utils.show.graph.text")) {
      override fun startEvaluation(callback: XFullValueEvaluationCallback) = executor.execute {
        ref.evaluateAsText()
        RConsoleManager.getInstance(ref.rInterop.project).currentConsoleOrNull?.executeActionHandler?.fireCommandExecuted()
        callback.evaluated("")
      }

      override fun isShowValuePopup() = false
    })
  }

  private fun setErrorPresentation(text: String, node: XValueNode) {
    node.setPresentation(AllIcons.Debugger.Db_obsolete, object : XValuePresentation() {
      override fun renderValue(renderer: XValueTextRenderer) {
        renderer.renderError(text.lines().first())
      }
    }, false)
    if (text.lines().size > 1) {
      node.setFullValueEvaluator(object : XFullValueEvaluator() {
        override fun startEvaluation(callback: XFullValueEvaluationCallback) {
          callback.evaluated(text)
        }
      })
    }
  }
}

private fun String.firstLine() = lineSequence().firstOrNull().orEmpty().trim()
