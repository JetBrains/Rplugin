package org.jetbrains.r.codeInsight.libraries

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.psi.PsiElement
import org.jetbrains.r.editor.completion.ARGUMENT_VALUE_PRIORITY
import org.jetbrains.r.editor.completion.RLookupElement
import org.jetbrains.r.editor.completion.RLookupElementFactory
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.isFunctionFromLibrarySoft

class Ggplot2SupportProvider : RLibrarySupportProvider {
  override fun completeArgument(context: PsiElement, completionConsumer: CompletionResultSet) {
    if (context !is RCallExpression) {
      return
    }

    if (context.isFunctionFromLibrarySoft("aes", "ggplot2")) {
      completeAes(context, completionConsumer)
    }
  }

  override fun completeArgumentValue(position: PsiElement, context: PsiElement, completionConsumer: CompletionResultSet) {
    if (context !is RCallExpression) {
      return
    }

    val parent = position.parent
    if ((context.isFunctionFromLibrarySoft("qplot", "ggplot2") || context.isFunctionFromLibrarySoft("stat_.*", "ggplot2")) &&
        (position is RStringLiteralExpression || position is RIdentifierExpression && position.name == DUMMY_IDENTIFIER_TRIMMED)
        && parent is RNamedArgument && parent.assignedValue == position && parent.name == "geom") {

      for (geom in GEOM_TYPES) {
        val lookupString = if (position is RIdentifierExpression) "\"" + geom + "\"" else geom
        val lookupElement = PrioritizedLookupElement.withPriority(RLookupElement(lookupString, true), ARGUMENT_VALUE_PRIORITY)
        completionConsumer.addElement(lookupElement)
      }
    }
  }

  private fun completeAes(call: RCallExpression, completionConsumer: CompletionResultSet) {
    val parent = call.parent
    var aesContainerCallName: String? = null
    if (parent is RArgumentList) {
      val aesContainerCall = parent.parent
      if (aesContainerCall is RCallExpression) {
        val aesContainerCallNameElement = aesContainerCall.expression
        if (aesContainerCallNameElement is RIdentifierExpression) {
          aesContainerCallName = aesContainerCallNameElement.name
        }
      }
    }
    if (!AES_NAMED_ARGUMENTS.containsKey(aesContainerCallName)) {
      aesContainerCallName = null
    }

    val presentedNamedArguments = call.argumentList.namedArgumentList.map { it.name }
    AES_NAMED_ARGUMENTS.getValue(aesContainerCallName).filter { !presentedNamedArguments.contains(it) }.forEach {
      completionConsumer.addElement(RLookupElementFactory.createNamedArgumentLookupElement(it))
    }
  }

  companion object {
    val AES_NAMED_ARGUMENTS = mapOf(
      "geom_bar" to listOf("alpha", "colour", "fill", "group", "linetype", "size"), // https://ggplot2.tidyverse.org/reference/geom_bar.html
      "geom_boxplot" to listOf("lower", "xlower", "upper", "xupper", "middle", "xmiddle", "ymin", "xmin", "ymax", "xmax", "alpha", "colour", "fill", "group", "linetype", "shape", "size", "weight"), // https://ggplot2.tidyverse.org/reference/geom_boxplot.html
      "geom_col" to listOf("alpha", "colour", "fill", "group", "linetype", "size"), // the same as geom_bar
      "geom_contour" to listOf("alpha", "colour", "group", "linetype", "size", "weight"), // https://ggplot2.tidyverse.org/reference/geom_contour.html
      "geom_contour_filled" to listOf("alpha", "colour", "fill", "group", "linetype", "size", "weight"), // https://ggplot2.tidyverse.org/reference/geom_contour.html
      "geom_density" to listOf("alpha", "colour", "fill", "group", "linetype", "size", "weight"), // https://ggplot2.tidyverse.org/reference/geom_density.html
      "geom_dotplot" to listOf("alpha", "colour", "fill", "group", "linetype", "stroke"), // https://ggplot2.tidyverse.org/reference/geom_dotplot.html
      "geom_jitter" to listOf("alpha", "colour", "fill", "group", "shape", "size", "stroke"), // https://ggplot2.tidyverse.org/reference/geom_jitter.html
      "geom_hex" to listOf("alpha", "colour", "fill", "group", "linetype", "size"), // https://ggplot2.tidyverse.org/reference/geom_hex.html
      "geom_histogram" to listOf("alpha", "colour", "fill", "group", "linetype", "size"), // the same as geom_bar
      "geom_linerange" to listOf("ymin", "xmin", "ymax", "xmax", "alpha", "colour", "group", "linetype", "size"), // https://ggplot2.tidyverse.org/reference/geom_linerange.html
      "geom_map" to listOf("map_id", "alpha", "colour", "fill", "group", "linetype", "size", "subgroup"), // https://ggplot2.tidyverse.org/reference/geom_map.html
      "geom_path" to listOf("alpha", "colour", "group", "linetype", "size"), // https://ggplot2.tidyverse.org/reference/geom_path.html
      "geom_point" to listOf("alpha", "colour", "fill", "group", "shape", "size", "stroke"), // https://ggplot2.tidyverse.org/reference/geom_point.html
      "geom_polygon" to listOf("alpha", "colour", "fill", "group", "linetype", "size", "subgroup"), // https://ggplot2.tidyverse.org/reference/geom_polygon.html
      "geom_ribbon" to listOf("xmin", "ymin", "xmax", "ymax", "alpha", "colour", "fill", "group", "linetype", "size"), // https://ggplot2.tidyverse.org/reference/geom_ribbon.html
      "geom_rug" to listOf("alpha", "colour", "group", "linetype", "size", "weight"), // https://ggplot2.tidyverse.org/reference/geom_rug.html
      "geom_quantile" to listOf("alpha", "colour", "group", "linetype", "size", "weight"), // https://ggplot2.tidyverse.org/reference/geom_quantile.html
      "geom_segment" to listOf("xend", "yend", "alpha", "colour", "group", "linetype", "size"), // https://ggplot2.tidyverse.org/reference/geom_segment.html
      "geom_smooth" to listOf("alpha", "colour", "fill", "group", "linetype", "size", "weight", "ymax", "ymin"), // https://ggplot2.tidyverse.org/reference/geom_smooth.html
      "geom_text" to listOf("label", "alpha", "angle", "colour", "family", "fontface", "group", "hjust", "lineheight", "size", "vjust"), // https://ggplot2.tidyverse.org/reference/geom_text.html
      "stat_contour" to listOf("z", "group", "order"), // https://ggplot2.tidyverse.org/reference/geom_contour.html
      "stat_contour_filled" to listOf("z", "fill", "group", "order"), // https://ggplot2.tidyverse.org/reference/geom_contour.html
      null to listOf("size", "stroke")
    )

    val GEOM_TYPES = AES_NAMED_ARGUMENTS.keys.filterNotNull().filter{it.startsWith("geom_")}.map {it.substring("geom_".length)}
  }
}