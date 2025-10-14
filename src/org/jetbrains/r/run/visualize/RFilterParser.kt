/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.google.protobuf.Empty
import com.intellij.r.psi.rinterop.DataFrameFilterRequest.Filter
import org.jetbrains.r.visualization.inlays.table.filters.IParser
import java.text.ParseException

class RFilterParser(val column: Int) : IParser {
  override fun parseText(expression: String): RRowFilter {
    if (expression.isBlank()) return EMPTY_FILTER
    when (expression.trim()) {
      "_" -> return RRowFilter(Filter.newBuilder().setNaFilter(Filter.NaFilter.newBuilder().setColumn(column).setIsNa(true)).build())
      "!_" -> return RRowFilter(Filter.newBuilder().setNaFilter(Filter.NaFilter.newBuilder().setColumn(column).setIsNa(false)).build())
    }
    val match = EXPRESSION_REGEX.matchEntire(expression)!!
    return RRowFilter(operands.getValue(match.groupValues[1]).invoke(match.groupValues[3]))
  }

  @Throws(ParseException::class)
  override fun parseInstantText(expression: String): IParser.InstantFilter {
    val result = IParser.InstantFilter()
    result.filter = parseText(expression)
    result.expression = expression
    return result
  }

  override fun stripHtml(expression: String) = expression

  override fun escape(expression: String) = expression

  private fun operatorFilter(type: Filter.Operator.Type, s: String): Filter {
    return Filter.newBuilder().setOperator(Filter.Operator.newBuilder().setColumn(column).setType(type).setValue(s)).build()
  }

  private val operands = mapOf<String, (String) -> Filter>(
    "=" to { s -> operatorFilter(Filter.Operator.Type.EQ, s) },
    "!" to { s -> operatorFilter(Filter.Operator.Type.NEQ, s) },
    "<" to { s -> operatorFilter(Filter.Operator.Type.LESS, s) },
    ">" to { s -> operatorFilter(Filter.Operator.Type.GREATER, s) },
    "<=" to { s -> operatorFilter(Filter.Operator.Type.LEQ, s) },
    ">=" to { s -> operatorFilter(Filter.Operator.Type.GEQ, s) },
    "~~" to { s -> operatorFilter(Filter.Operator.Type.REGEX, "^$s$") },
    "~" to { s -> operatorFilter(Filter.Operator.Type.REGEX, patternToRegex(s)) },
    "!~" to { s -> notFilter(operatorFilter(Filter.Operator.Type.REGEX, patternToRegex(s))) },
    "" to { s -> operatorFilter(Filter.Operator.Type.REGEX, patternToRegex("*$s*")) }
  )

  companion object {
    private fun patternToRegex(s: String): String {
      val sb = StringBuilder("^")
      var escaped = false
      for (c in s) {
        when (c) {
          '*' -> if (escaped) {
            sb.append("\\*")
            escaped = false
          } else {
            sb.append(".*")
          }
          '?' -> if (escaped) {
            sb.append("\\?")
            escaped = false
          } else {
            sb.append(".")
          }
          '\\' -> {
            if (escaped) {
              sb.append("\\\\\\\\")
            }
            escaped = !escaped
          }
          else -> {
            if (escaped) {
              sb.append("\\\\")
              escaped = false
            }
            when (c) {
              '[', ']', '^', '$', '+', '{', '}', '|', '(', ')', '.' -> sb.append('\\').append(c)
              else -> sb.append(c)
            }
          }
        }
      }
      if (escaped) {
        sb.append("\\\\")
      }
      sb.append("$")
      return sb.toString()
    }

    private fun notFilter(filter: Filter): Filter {
      return Filter.newBuilder()
        .setComposed(Filter.ComposedFilter.newBuilder().setType(Filter.ComposedFilter.Type.NOT).addFilters(filter)).build()
    }

    private val EXPRESSION_REGEX = Regex("\\s*(>=|<=|<>|!~|~~|>|<|=|~|!)?(\\s*(.*))", RegexOption.DOT_MATCHES_ALL)
    val EMPTY_FILTER = RRowFilter(Filter.newBuilder().setTrue(Empty.getDefaultInstance()).build())
  }
}
