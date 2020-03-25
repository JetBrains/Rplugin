// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug.stack

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.xdebugger.frame.XFullValueEvaluator
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.presentation.XValuePresentation
import org.jetbrains.r.RBundle
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.packages.RequiredPackageException
import org.jetbrains.r.packages.RequiredPackageInstaller
import org.jetbrains.r.rinterop.*
import org.jetbrains.r.run.visualize.RDataFrameException
import org.jetbrains.r.run.visualize.RVisualizeTableUtil
import org.jetbrains.r.util.tryRegisterDisposable

private abstract class RXValuePresentationBase(val v: RXVar) : XValuePresentation() {
  abstract fun renderValueImpl(renderer: XValueTextRenderer)

  override fun renderValue(renderer: XValueTextRenderer) {
    if (v.stackFrame.variableViewSettings.showSize) {
      v.objectSize?.let { renderer.renderComment("(${StringUtil.formatFileSize(it)}) ") }
    }
    renderValueImpl(renderer)
  }

  override fun getType(): String? {
    return if (v.stackFrame.variableViewSettings.showClasses && v.rVar.value.cls.isNotEmpty()) {
      v.rVar.value.cls.joinToString(", ")
    } else {
      null
    }
  }
}

private open class RXValuePresentation(v: RXVar, val text: String) : RXValuePresentationBase(v) {
  override fun renderValueImpl(renderer: XValueTextRenderer) {
    renderer.renderValue(text)
  }
}

internal object RXPresentationUtils {
  fun setPresentation(node: XValueNode, rxVar: RXVar) {
    when (val value = rxVar.rVar.value) {
      is RValueUnevaluated -> setPromisePresentation(node, value, rxVar)
      is RValueSimple -> setVarPresentation(node, value, rxVar)
      is RValueDataFrame -> setDataFramePresentation(node, value, rxVar)
      is RValueList -> setListPresentation(node, value, rxVar)
      is RValueFunction -> setFunctionPresentation(node, value, rxVar)
      is RValueEnvironment -> setEnvironmentPresentation(node, value, rxVar)
      is RValueGraph -> setGraphPresentation(node, value, rxVar)
      is RValueError -> setErrorPresentation(node, value, rxVar)
    }
  }

  fun setEnvironmentPresentation(node: XValueNode, rValue: RValueEnvironment? = null, rxVar: RXVar? = null) {
    if (rxVar == null) {
      node.setPresentation(AllIcons.Debugger.Value, object : XValuePresentation() {
        override fun renderValue(renderer: XValueTextRenderer) {}
        override fun getSeparator() = ""
      }, true)
    } else {
      val name = rValue?.envName?.takeIf { it.isNotEmpty() } ?: RBundle.message("rx.presentation.utils.environment.unnamed")
      node.setPresentation(AllIcons.Debugger.Value, RXValuePresentation(rxVar, name), true)
    }
  }

  private fun setPromisePresentation(node: XValueNode, rValue: RValueUnevaluated, rxVar: RXVar) {
    node.setPresentation(AllIcons.Nodes.Unknown, object : RXValuePresentationBase(rxVar) {
      override fun renderValueImpl(renderer: XValueTextRenderer) {
        renderer.renderComment(rValue.code)
      }
    }, false)
    node.setFullValueEvaluator(object : XFullValueEvaluator(RBundle.message("rx.presentation.utils.evaluate.link.text")) {
      override fun startEvaluation(callback: XFullValueEvaluationCallback) {
        val ref = rxVar.rVar.ref
        ref.getValueInfoAsync()
          .also { rxVar.stackFrame.tryRegisterDisposable(Disposable { it.cancel() }) }
          .then {
            ref.rInterop.invalidateCaches()
            RConsoleManager.getInstance(ref.rInterop.project).currentConsoleOrNull?.executeActionHandler?.fireCommandExecuted()
            callback.evaluated("")
          }
      }

      override fun isShowValuePopup() = false
    })
  }

  private fun setFunctionPresentation(node: XValueNode, rValue: RValueFunction, rxVar: RXVar) {
    node.setPresentation(AllIcons.Nodes.Function, RXValuePresentation(rxVar, rValue.header.firstLine()), false)
    node.setFullValueEvaluator(object : XFullValueEvaluator(RBundle.message("rx.presentation.utils.view.code.link.text")) {
      override fun startEvaluation(callback: XFullValueEvaluationCallback) {
        val ref = rxVar.rVar.ref
        ref.functionSourcePositionAsync().then {
          ApplicationManager.getApplication().invokeLater {
            it?.xSourcePosition?.createNavigatable(ref.rInterop.project)?.navigate(true)
            callback.evaluated("")
          }
        }
      }

      override fun isShowValuePopup() = false
    })
  }

  private fun setDataFramePresentation(node: XValueNode, rValue: RValueDataFrame, rxVar: RXVar) {
    node.setPresentation(AllIcons.Nodes.DataTables,
                         RXValuePresentation(rxVar, RBundle.message("rx.presentation.utils.data.frame.text", rValue.rows, rValue.cols)),
                         true)
    node.setFullValueEvaluator(object : XFullValueEvaluator(RBundle.message("rx.presentation.utils.view.table.link.text")) {
      override fun startEvaluation(callback: XFullValueEvaluationCallback) {
        val ref = rxVar.rVar.ref
        ref.rInterop.dataFrameGetViewer(ref).onSuccess {
          RVisualizeTableUtil.showTable(ref.rInterop.project, it, rxVar.name)
        }.onError {
          when (it) {
            is RDataFrameException -> callback.errorOccurred(it.message.orEmpty())
            is RequiredPackageException -> {
              RequiredPackageInstaller.getInstance(ref.rInterop.project).installPackagesWithUserPermission(
                RBundle.message("rx.presentation.utils.view.table.utility.name"), it.missingPackages)
            }
          }
        }.onProcessed {
          callback.evaluated("")
        }
      }

      override fun isShowValuePopup() = false
    })
  }

  private fun setListPresentation(node: XValueNode, rValue: RValueList, rxVar: RXVar) {
    val text = if (rValue.length == 0L) {
      RBundle.message("rx.presentation.utils.empty.list.text")
    } else {
      RBundle.message("rx.presentation.utils.list.text", rValue.length)
    }
    node.setPresentation(AllIcons.Debugger.Db_array, RXValuePresentation(rxVar, text), rValue.length > 0)
  }

  private fun setVarPresentation(node: XValueNode, rValue: RValueSimple, rxVar: RXVar) {
    var line = rValue.text.firstLine()
    if (!rValue.isComplete && rValue.text == line) {
      line += " ..."
    }
    if (line.startsWith("[1]")) {
      line = line.drop(3).trimStart()
    }
    node.setPresentation(AllIcons.Debugger.Db_primitive, object : RXValuePresentation(rxVar, line) {
      override fun getSeparator() = if (line.isNotEmpty() || type != null) super.getSeparator() else ""
    }, rValue.isVector)
    if (rValue.text.contains('\n') || !rValue.isComplete) {
      node.setFullValueEvaluator(object : XFullValueEvaluator() {
        override fun startEvaluation(callback: XFullValueEvaluationCallback) {
          if (rValue.isComplete) {
            callback.evaluated(rValue.text)
          } else {
            rxVar.rVar.ref.evaluateAsTextAsync()
              .also { rxVar.stackFrame.tryRegisterDisposable(Disposable { it.cancel() }) }
              .then { callback.evaluated(it) }
          }
        }
      })
    }
  }

  private fun setGraphPresentation(node: XValueNode, rValue: RValueGraph, rxVar: RXVar) {
    node.setPresentation(AllIcons.Nodes.PpLib, RXValuePresentation(rxVar, RBundle.message("rx.presentation.utils.graph.text")), true)
    node.setFullValueEvaluator(object : XFullValueEvaluator(RBundle.message("rx.presentation.utils.show.graph.text")) {
      override fun startEvaluation(callback: XFullValueEvaluationCallback) {
        val ref = rxVar.rVar.ref
        ref.evaluateAsTextAsync()
          .also { rxVar.stackFrame.tryRegisterDisposable(Disposable { it.cancel() }) }
          .then {
            RConsoleManager.getInstance(ref.rInterop.project).currentConsoleOrNull?.executeActionHandler?.fireCommandExecuted()
            callback.evaluated("")
          }
      }

      override fun isShowValuePopup() = false
    })
  }

  private fun setErrorPresentation(node: XValueNode, rValue: RValueError, rxVar: RXVar) {
    val text = rValue.text
    node.setPresentation(AllIcons.Debugger.Db_obsolete, object : RXValuePresentationBase(rxVar) {
      override fun renderValueImpl(renderer: XValueTextRenderer) {
        renderer.renderError(text.firstLine())
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
