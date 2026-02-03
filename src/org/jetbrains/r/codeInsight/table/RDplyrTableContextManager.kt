package org.jetbrains.r.codeInsight.table

import com.intellij.r.psi.psi.DplyrFunction
import com.intellij.r.psi.psi.RDplyrAnalyzer
import com.intellij.r.psi.psi.TableColumnInfo
import com.intellij.r.psi.psi.TableManipulationAnalyzer
import com.intellij.r.psi.psi.TableManipulationContextInfo
import com.intellij.util.Processor

class RDplyrTableContextManager : RTableManipulationAnalyzerManager<DplyrFunction>() {
  override fun getTableManipulationAnalyzer(): TableManipulationAnalyzer<DplyrFunction> {
    return RDplyrAnalyzer
  }

  override fun processCurrentColumns(tableContextInfo: TableManipulationContextInfo<DplyrFunction>,
                                     processor: Processor<TableColumnInfo>): Boolean {
    return RDplyrAnalyzer.processCurrentColumns(tableContextInfo.callInfo, tableContextInfo.currentTableArgument, processor)
  }
}