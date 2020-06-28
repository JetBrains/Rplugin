// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.documentation

import com.intellij.codeInsight.documentation.DocumentationManagerProtocol
import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo.isWindows
import com.intellij.openapi.util.text.StringUtil
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.RBundle
import org.jetbrains.r.RLanguage
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.editor.completion.RLookupElement
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageInstaller
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.refactoring.RNamesValidator
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.getWithCheckCanceled
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

private const val INDEX_HTML = "00Index.html"
private const val WINDOWS_MAX_PATH_LENGTH = 259
private const val INSTALL_REQUIRED_PACKAGES_LINK = "#Install#"
private val LOG = Logger.getInstance(RDocumentationProvider::class.java)

/**
 * For local function definitions provide doc string documentation (using docstring)
 * For library functions use R help.
 */
class RDocumentationProvider : AbstractDocumentationProvider() {

  private val keywords = listOf("TRUE", "FALSE", "NULL", "NA", "Inf", "NaN", "NA_integer_", "NA_real_", "NA_complex_", "NA_character_",
                                "if", "else", "repeat", "while", "function", "return", "for", "in", "next", "break", "...")
  private val brackets = listOf("(", ")", "[", "]", "[[", "]]", "{", "}", ",", ";")

  private val separator = File.separator

  private val pathToDocumentation = makePath(PathManager.getSystemPath(), "documentation")

  private val localFunctionRequiredPackage = listOf(RequiredPackage("roxygen2"))

  override fun getCustomDocumentationElement(editor: Editor, file: PsiFile, contextElement: PsiElement?): PsiElement? {
    if (contextElement == null || contextElement.language != RLanguage.INSTANCE) return null

    if (hasNoDocumentation(contextElement) && editor.caretModel.currentCaret.offset == contextElement.textRange.startOffset) {
      return getElementForDocumentation(PsiTreeUtil.prevLeaf(contextElement))
    }

    return getElementForDocumentation(contextElement)
  }

  private fun getElementForDocumentation(contextElement: PsiElement?): PsiElement? {
    val elementText = contextElement?.text ?: return null
    return when {
      hasNoDocumentation(contextElement) -> null
      contextElement is RPsiElement -> contextElement
      elementText == "%%" || elementText == "%/%" -> contextElement.parent
      elementText.startsWith("%") -> // resolve to infix operator reference
        contextElement.parent.reference?.resolve()
      keywords.contains(elementText) -> contextElement
      else -> contextElement.parent
    }
  }

  private fun hasNoDocumentation(contextElement: PsiElement): Boolean {
    val elementText = contextElement.text
    return elementText.isBlank() ||
           elementText.startsWith("#") ||
           elementText.all { it.isDigit() } ||
           elementText in brackets
  }

  @Throws(RequiredPackageException::class)
  private fun getHtmlLocalFunctionPath(rInterop: RInterop,
                                       localFunction: RAssignmentStatement,
                                       docStringValue: String?): String? {
    // TODO(This is a temporary solution, full support for roxygen see DS-16)
    docStringValue ?: return null

    val missingPackages = RequiredPackageInstaller.getInstance(localFunction.project)
      .getMissingPackages(localFunctionRequiredPackage)

    if (missingPackages.isNotEmpty()) {
      val buffer = StringBuilder()
        .append("<p>")
        .append(RBundle.message("documentation.local.function.missing.packages"))
      for (missingPackage in missingPackages) {
        buffer.append("<br>").append(missingPackage.name)
      }

      buffer.append("<br>")
      DocumentationManagerUtil.createHyperlink(buffer, localFunction, INSTALL_REQUIRED_PACKAGES_LINK,
                                               RBundle.message("documentation.local.function.missing.packages.install.link"), true)
      throw RequiredPackageException(buffer.toString())
    }

    val fileName = localFunction.containingFile.name
    val functionName = localFunction.name
    val htmlPath = cutPathIfWindows(makePath(pathToDocumentation, "local", fileName, "$functionName.html"), WINDOWS_MAX_PATH_LENGTH)
    val htmlFile = File(htmlPath)
    if (htmlFile.exists()) {
      htmlFile.delete()
    }

    val functionText = "#'${docStringValue.replace("<br>", "\n#'")}\n${localFunction.text.takeWhile { it != '{' }}{}"
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")

    val rdPath = cutPathIfWindows(makePath(pathToDocumentation, "local", "rd.tmp"), WINDOWS_MAX_PATH_LENGTH)
    val rdFile = File(rdPath)

    val htmlTmpPath = cutPathIfWindows(makePath(pathToDocumentation, "local", fileName, "${functionName}_tmp"), WINDOWS_MAX_PATH_LENGTH - 1)
    val htmlTmpFile = File(htmlTmpPath)
    try {
      if (!htmlFile.parentFile.exists()) {
        Files.createDirectories(htmlFile.parentFile.toPath())
      }

      rdFile.delete()
      val executionMakeRdFromRoxygen = rInterop.makeRdFromRoxygen(functionName, functionText, rdFile.absolutePath.replace(separator, "/"))
      if (!rdFile.exists()) {
        LOG.error(
          "Convert roxygen to Rd failed. Stdout: ${executionMakeRdFromRoxygen.stdout};\n Stderr: ${executionMakeRdFromRoxygen.stderr}")
        return null
      }

      val executionMakeHtml = rInterop.convertRd2HTML(rdFilePath = rdFile.absolutePath.replace(separator, "/"),
                                                      outputFilePath = htmlTmpFile.absolutePath.replace(separator, "/"))
      if (!convertHelpPage(htmlTmpPath, htmlPath)) {
        LOG.error(
          "Roxygen2Rd. Stdout: ${executionMakeRdFromRoxygen.stdout};\n Stderr: ${executionMakeRdFromRoxygen.stderr}\n\n" +
          "Rd2HTML. Stdout: ${executionMakeHtml.stdout};\n Stderr: ${executionMakeHtml.stderr}")
        return null
      }
      return htmlPath
    }
    catch (e: IOException) {
      LOG.error(e)
      return null
    }
    finally {
      htmlTmpFile.delete()
      rdFile.delete()
    }
  }

  @Throws(RequiredPackageException::class)
  private fun getDocumentationLocalFunction(rInterop: RInterop, reference: PsiElement): String? {
    // locally defined function definitions
    var localFunction = reference

    if (localFunction is RIdentifierExpression) {
      if (localFunction.parent is RAssignmentStatement) {
        localFunction = localFunction.parent
      }
      else if (localFunction.parent is RCallExpression) {
        val resolves = (localFunction.reference as PsiPolyVariantReference).multiResolve(false)
        for (resolve in resolves) {
          if (resolve.element is RAssignmentStatement) {
            localFunction = resolve.element as RAssignmentStatement
            break
          }
        }
      }
    }

    if (localFunction is RAssignmentStatement) {
      val assignedValue = localFunction.assignedValue
      if (assignedValue is RFunctionExpression) {
        return getHtmlLocalFunctionPath(rInterop, localFunction, assignedValue.docStringValue)
      }
    }

    return null
  }

  private inline fun checkPossibilityReturnDocumentation(reference: PsiElement, exitWithReport: (message: String?) -> Unit) {
    val containingFile = reference.containingFile

    if (containingFile.language != RLanguage.INSTANCE) {
      exitWithReport(null)
    }

    containingFile.runtimeInfo ?: exitWithReport(RBundle.message("documentation.console.closed.problem"))
  }

  private fun makePath(vararg pathParts: String): String {
    val resultPath = StringJoiner(separator, "", "")
    for (part in pathParts) {
      resultPath.add(part)
    }
    return resultPath.toString()
  }

  private fun cutPathIfWindows(path: String, length: Int): String {
    if (isWindows) {
      return path.take(length)
    }
    return path
  }

  override fun generateDoc(psiElement: PsiElement?, identifier: PsiElement?): String? {
    if (psiElement == null || psiElement.language != RLanguage.INSTANCE) return null
    RDocumentationUtil.getTextFromElement(psiElement)?.let { return it }
    val rInterop = psiElement.containingFile.runtimeInfo?.rInterop ?: return null
    if (psiElement is RStringLiteralExpression && psiElement.getCopyableUserData(INTERCEPTED_LINK) == true) {
      return rInterop.httpdRequest(psiElement.name.orEmpty())?.let { RDocumentationUtil.convertHelpPage(it) }
    }
    checkPossibilityReturnDocumentation(psiElement) { return it }
    try {
      getDocumentationLocalFunction(rInterop, psiElement)?.let { path ->
        return Scanner(File(path), StandardCharsets.UTF_8).use {
          it.useDelimiter("\\A").next()
        }
      }
    } catch (e: RequiredPackageException) {
      return e.message
    }

    val element = PsiTreeUtil.getNonStrictParentOfType(psiElement, RExpression::class.java) ?: return null
    val (symbol, pkg) = when {
      element is RStringLiteralExpression || element is RNamedArgument -> return null
      element is RHelpExpression -> "help" to null
      element is RNamespaceAccessExpression -> (element.identifier?.name ?: return null) to element.namespaceName
      element.parent is RNamespaceAccessExpression -> {
        val parent = (element.parent as RNamespaceAccessExpression)
        (parent.identifier?.name ?: return null) to parent.namespaceName
      }
      element is RCallExpression -> {
        when (val elementFirstChild = element.firstChild) {
          is RNamespaceAccessExpression -> (elementFirstChild.identifier?.name ?: return null) to elementFirstChild.namespaceName
          is RIdentifierExpression -> elementFirstChild.name to null
          else -> elementFirstChild.text to null
        }
      }
      element is RAssignmentStatement -> element.name to null
      element is RIdentifierExpression -> element.name to null
      else -> element.text to null
    }
    val response = rInterop.getDocumentationForSymbol(symbol, pkg).getWithCheckCanceled() ?: return null
    return RDocumentationUtil.convertHelpPage(response)
  }

  /**
   * Intercepts clicks in documentation popup if link starts with psi_element://
   *
   *
   * See https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000095710-Intercept-clicks-in-documentation-popup-
   *
   * @param psiManager
   * @param link
   * @param context
   * @return
   */
  override fun getDocumentationElementForLink(psiManager: PsiManager, link: String?, context: PsiElement?): PsiElement? {
    if (context == null || context.language != RLanguage.INSTANCE || link == null) return null
    if (link == INSTALL_REQUIRED_PACKAGES_LINK) {
      RequiredPackageInstaller.getInstance(psiManager.project)
        .installPackagesWithUserPermission(RBundle.message("documentation.utility.name"), localFunctionRequiredPackage, false)
      return null
    }
    return RElementFactory.createRPsiElementFromText(psiManager.project, "\"${StringUtil.escapeStringCharacters(link)}\"").also {
      it.putCopyableUserData(INTERCEPTED_LINK, true)
    }
  }

  override fun getDocumentationElementForLookupItem(psiManager: PsiManager?, item: Any?, element: PsiElement?): PsiElement? {
    if (psiManager == null || item !is RLookupElement) return null
    val name = item.lookup
    val pkg = item.packageName
    if (pkg == null) return RElementFactory.createRPsiElementFromText(psiManager.project, RNamesValidator.quoteIfNeeded(name))
    return RElementFactory.createRPsiElementFromText(
      psiManager.project, "${RNamesValidator.quoteIfNeeded(pkg)}::${RNamesValidator.quoteIfNeeded(name)}")
  }

  private fun convertHelpPage(fromPath: String, toPath: String): Boolean {
    try {

      val file = File(fromPath)
      val htmlRaw = try {
        Scanner(file, StandardCharsets.UTF_8).use { it.useDelimiter("\\A").next() }
      }
      catch (e: NoSuchElementException) {
        return false
      }

      val headBody = htmlRaw.indexOf("<body>")

      var htmlTrimmed = htmlRaw
      if (headBody != -1) {
        htmlTrimmed = htmlRaw.substring(headBody + 6, htmlRaw.length).trim { it <= ' ' }
      }
      else if (htmlRaw.startsWith("<!DOCTYPE html")) {
        return false
      }

      // Remove not html hrefs
      htmlTrimmed = htmlTrimmed.replace(Regex("((<\\w+>)?<a href=\"(?!http)(?>(?!\\.html).)*\">.*</a>.*(<\\w+>)?)"), "")

      // Remove manuals hrefs
      htmlTrimmed = htmlTrimmed.replace(Regex("((<\\w+>)?<a href=\".*/doc/.*\">.*</a>.*(<\\w+>)?)"), "")

      // Remove images
      htmlTrimmed = htmlTrimmed.replace(Regex("<a href.*<img.*></a>"), "")
      htmlTrimmed = htmlTrimmed.replace(Regex("<img.*>"), "")

      // fix relative URLs
      htmlTrimmed = htmlTrimmed.replace("../../", DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL)
      htmlTrimmed = htmlTrimmed.replace("../", "${DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL}${file.parentFile.name}/html/")

      if (File(toPath).name == INDEX_HTML) {
        htmlTrimmed = htmlTrimmed.replace(Regex("(href=\")((?!psi)(?!http))"),
                                          "href=\"${DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL}${file.parentFile.name}/html/")
      }
      htmlTrimmed = htmlTrimmed.replace(Regex("(href=\")(?!psi)"), "href=\"${DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL}")

      // Replace links with internal ones that are correctly handled by
      // com.intellij.codeInsight.documentation.DocumentationManager.navigateByLink()
      // See https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000095710-Intercept-clicks-in-documentation-popup-
      //      htmlTrimmed = htmlTrimmed.replace("http://127.0.0.1:$HELP_SERVER_PORT/", "psi_element://")
      //            http://127.0.0.1:25593/library/base/html/file.info.html
      //            /library/base/html/path.info.html

      // Add index page

      val parentName = file.parentFile.name
      val endBody = htmlTrimmed.indexOf("</body>")
      htmlTrimmed = htmlTrimmed.substring(0, endBody) +
                    "<hr>\n<div style=\"text-align: center;\">[Package <em>" +
                    "${parentName}</em> <a href=\"psi_element://${parentName}/00Index.html\">Index</a>]</div>\n" +
                    htmlTrimmed.substring(endBody)


      LOG.info(htmlTrimmed)

      File(toPath).writeText(htmlTrimmed, StandardCharsets.UTF_8)
      return true
    }
    catch (e: IOException) {
      LOG.error(e)
      return false
    }
  }

  private class RequiredPackageException(message: String?) : RuntimeException(message)

  companion object {
    private val INTERCEPTED_LINK = Key<Boolean>("org.jetbrains.r.documentation.InterceptedLink")
  }
}
