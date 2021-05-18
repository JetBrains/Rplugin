package org.jetbrains.r.codeInsight.table

import com.intellij.util.CommonProcessors
import org.apache.commons.lang.StringUtils
import org.jetbrains.r.psi.TableColumnInfo

open class RTableColumnCollectProcessor : CommonProcessors.CollectProcessor<TableColumnInfo>() {
  override fun getResults(): MutableCollection<TableColumnInfo> {
    return super.getResults().groupBy { it.name }
      .map { (name, list) ->
        list.groupBy { it.quoteNeeded }.map {(quoteNeeded, list) -> TableColumnInfo(name,
                                                                                    StringUtils.join(list.mapNotNull { it.type }.toSet(), "/"),
                                                                                    definition = list.mapNotNull { it.definition }.firstOrNull(),
                                                                                    quoteNeeded = quoteNeeded)}

      }.flatten().toMutableList()
  }
}