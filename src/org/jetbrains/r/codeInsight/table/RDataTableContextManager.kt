package org.jetbrains.r.codeInsight.table

import org.jetbrains.r.psi.DataTableFunction
import org.jetbrains.r.psi.RDataTableAnalyzer
import org.jetbrains.r.psi.TableManipulationAnalyzer

class RDataTableContextManager : RTableManipulationAnalyzerManager<DataTableFunction>() {
  override fun getTableManipulationAnalyzer(): TableManipulationAnalyzer<DataTableFunction> {
    return RDataTableAnalyzer
  }
}