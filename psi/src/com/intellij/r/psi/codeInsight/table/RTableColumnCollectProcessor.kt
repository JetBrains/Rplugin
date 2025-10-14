package com.intellij.r.psi.codeInsight.table

import com.intellij.openapi.util.text.StringUtil
import com.intellij.r.psi.psi.TableColumnInfo
import com.intellij.util.CommonProcessors

open class RTableColumnCollectProcessor : CommonProcessors.CollectProcessor<TableColumnInfo>() {
  override fun getResults(): MutableCollection<TableColumnInfo> {
    return super.getResults().groupBy { it.name }
      .map { (name, list) ->
        list.groupBy { it.quoteNeeded }.map {(quoteNeeded, list) -> TableColumnInfo(name,
                                                                                    StringUtil.join(list.mapNotNull { it.type }.toSet(), "/"),
                                                                                    definition = list.firstNotNullOfOrNull { it.definition },
                                                                                    quoteNeeded = quoteNeeded)}

      }.flatten().toMutableList()
  }
}