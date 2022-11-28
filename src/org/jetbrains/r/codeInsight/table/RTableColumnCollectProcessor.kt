package org.jetbrains.r.codeInsight.table

import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.CommonProcessors
import org.jetbrains.r.psi.TableColumnInfo

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