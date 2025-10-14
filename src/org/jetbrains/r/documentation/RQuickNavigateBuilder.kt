package org.jetbrains.r.documentation

import com.intellij.lang.documentation.QuickDocHighlightingHelper
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.RLanguage
import com.intellij.r.psi.packages.RSkeletonUtilPsi
import com.intellij.r.psi.psi.RPsiUtil
import com.intellij.r.psi.psi.RSkeletonParameterPomTarget
import com.intellij.r.psi.psi.api.RAssignmentStatement
import com.intellij.r.psi.psi.api.RIdentifierExpression
import com.intellij.r.psi.psi.api.RParameter
import com.intellij.r.psi.psi.api.RPsiElement
import com.intellij.r.psi.refactoring.quoteIfNeeded
import com.intellij.r.psi.refactoring.rNamesValidator
import com.intellij.r.psi.rinterop.RReference
import com.intellij.r.psi.rinterop.RValueDataFrame
import com.intellij.ui.ColorUtil
import org.jetbrains.r.console.runtimeInfo

object RQuickNavigateBuilder {

  fun getQuickNavigationInfo(element: PsiElement?, originalElement: PsiElement?): String? {
    if (element == null || element !is RPsiElement) return null
    val project = element.project
    return CachedValuesManager.getManager(project).getCachedValue(element) {
      CachedValueProvider.Result.create(getQuickNavigationInfoInner(element), PsiModificationTracker.MODIFICATION_COUNT)
    }
  }

  private fun getQuickNavigationInfoInner(element: PsiElement): String? {
    val htmlBuilder = StringBuilder()
    val file =
      if (element is PomTargetPsiElement) {
        val target = element.target
        if (target is RSkeletonParameterPomTarget) target.assignment.containingFile
        else null
      }
      else element.containingFile

    file?.let { htmlBuilder.addFileOrPackageInfoIfNeeded(it) }
    when (element) {
      is PomTargetPsiElement -> {
        val target = element.target
        if (target is RSkeletonParameterPomTarget) {
          htmlBuilder.addParameterInfo(target)
        }
      }
      is RAssignmentStatement -> {
        htmlBuilder.addAssignmentInfo(element)
      }
      is RParameter -> {
        htmlBuilder.addParameterInfo(element) { return null }
      }
      is RIdentifierExpression -> {
        htmlBuilder.addIdentifierInfo(element) { return null }
      }
    }
    return htmlBuilder.toString().takeIf { it.isNotEmpty() }
  }

  private fun StringBuilder.addAssignmentInfo(element: RAssignmentStatement) {
    if (element.isFunctionDeclaration) addFunctionAssignmentInfo(element)
    else addVariableAssignmentInfo(element)
  }

  private fun StringBuilder.addFunctionAssignmentInfo(element: RAssignmentStatement) {
    val quotedName = rNamesValidator.quoteIfNeeded(element.name, element.project)
    val prefix = "$quotedName <- function"
    val text = "$prefix${element.functionParameters.replace("\\s{2,}".toRegex(), "\n")}"
    val html = QuickDocHighlightingHelper.getStyledCodeFragment(element.project, RLanguage.INSTANCE, text)
    addPatchedMultiLineFunctionDeclarationHtml(html)
  }

  private fun StringBuilder.addVariableAssignmentInfo(element: RAssignmentStatement) {
    append("<div>")
    val name = element.name
    val fixedName by lazy { fixHtmlExtraSymbols(name) }
    when {
      element.isPrimitiveFunctionDeclaration -> {
        append(RBundle.message("quick.navigate.info.primitive.text", fixedName))
      }
      RPsiUtil.isLibraryElement(element) -> {
        append(RBundle.message("quick.navigate.info.dataframe.text"))
        val interop = element.containingFile.runtimeInfo?.rInterop
        if (interop != null) {
          val dataFrameInfo = RReference.expressionRef(name, interop).getValueInfo() as? RValueDataFrame
          if (dataFrameInfo != null) {
            append("$SPACE[").append(dataFrameInfo.rows).append("x").append(dataFrameInfo.cols).append("]")
          }
        }
      }
      else -> {
        append(RBundle.message("quick.navigate.info.variable.text", fixedName))
      }
    }
    append("</div>")
  }

  private fun StringBuilder.addParameterInfo(pomParameter: RSkeletonParameterPomTarget) {
    append("<div>")
    val assignment = pomParameter.assignment
    val functionName = fixHtmlExtraSymbols(assignment.name)
    val parameterName = fixHtmlExtraSymbols(pomParameter.name)
    append(RBundle.message("quick.navigate.info.parameter.text", parameterName, functionName))
    append("</div>")
  }

  private inline fun StringBuilder.addParameterInfo(parameter: RParameter, onFail: () -> Unit) {
    val assignment = PsiTreeUtil.getParentOfType(parameter, RAssignmentStatement::class.java)
    if (assignment == null) {
      onFail()
      return
    }
    append("<div>")
    val functionName = fixHtmlExtraSymbols(assignment.name)
    val parameterName = fixHtmlExtraSymbols(parameter.name)
    append(RBundle.message("quick.navigate.info.parameter.text", parameterName, functionName))
    append("</div>")
  }

  private inline fun StringBuilder.addIdentifierInfo(identifier: RIdentifierExpression, onFail: () -> Unit) {
    val assignment = identifier.parent as? RAssignmentStatement
    val name by lazy { fixHtmlExtraSymbols(identifier.name) }
    if (assignment == null) {
      if (identifier.parent is RParameter) {
        PsiTreeUtil.getParentOfType(identifier, RAssignmentStatement::class.java)?.let { function ->
          val functionName = fixHtmlExtraSymbols(function.name)
          append("<div>")
          append(RBundle.message("quick.navigate.info.parameter.declaration.text", name, functionName))
          append("</div>")
          return
        }
      }
      onFail()
      return
    }
    append("<div>")
    if (assignment.isFunctionDeclaration) {
      append(RBundle.message("quick.navigate.info.function.declaration.text", name))
    }
    else {
      append(RBundle.message("quick.navigate.info.variable.declaration.text", name))
    }
    append("</div>")
  }

  /**
   * Presents function declaration as table for indent arguments
   */
  private fun StringBuilder.addPatchedMultiLineFunctionDeclarationHtml(html: String) {
    var lines = html.split("<br></span>")
    lines = lines.mapIndexed { ind, line ->
      if (ind == lines.size - 1) line
      else "$line</span>"
    }
    append("<table cellspacing=\"0\" cellpadding=\"0\">")
    lines.forEachIndexed { ind, line ->
      append("<tr>")
      append("<td>")
      if (ind == 0) {
        val assignInd = line.indexOf("</span>", line.indexOf("function")) + "</span>".length
        append(line.substring(0, assignInd))
        append("</td><td>")
        append(line.substring(assignInd))
      }
      else {
        append("</td><td>")
        val textBeginInd = "<span.*?>".toRegex().find(line)!!.range.last + 1
        append(line.substring(0, textBeginInd))
        append(SPACE)
        append(line.substring(textBeginInd))
      }
      append("</td>")
      append("</tr>")
    }
    append("</table>")
  }

  private fun StringBuilder.addFileOrPackageInfoIfNeeded(file: PsiFile) {
    val rPackage = RSkeletonUtilPsi.skeletonFileToRPackage(file)
    val fileName: String =
      if (rPackage != null) "${rPackage.name} (${rPackage.version})"
      else file.name

    append("<div style=\"color:#$NOT_USED_ELEMENT_COLOR\">")
    append(fixHtmlExtraSymbols(fileName))
    append("</div>")
  }

  private fun fixHtmlExtraSymbols(text: String): String {
    return buildString {
      text.forEach {
        when (it) {
          '<' -> append("&lt;")
          '>' -> append("&gt;")
          '&' -> append("&amp;")
          ' ' -> append(SPACE)
          '\n' -> append("<br>")
          else -> append(it)
        }
      }
    }
  }

  private const val SPACE = "&#32;"
  private val NOT_USED_ELEMENT_COLOR = ColorUtil.toHex(CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES.defaultAttributes.foregroundColor)
}