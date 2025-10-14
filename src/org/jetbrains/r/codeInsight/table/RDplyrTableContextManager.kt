package org.jetbrains.r.codeInsight.table

import com.intellij.r.psi.psi.*
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