package org.jetbrains.r.codeInsight.table

import com.intellij.r.psi.psi.DataTableFunction
import com.intellij.r.psi.psi.RDataTableAnalyzer
import com.intellij.r.psi.psi.TableManipulationAnalyzer

class RDataTableContextManager : RTableManipulationAnalyzerManager<DataTableFunction>() {
  override fun getTableManipulationAnalyzer(): TableManipulationAnalyzer<DataTableFunction> {
    return RDataTableAnalyzer
  }
}