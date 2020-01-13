// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.documentation

import com.google.common.base.CharMatcher
import com.intellij.codeInsight.documentation.DocumentationManagerProtocol
import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.SystemInfo.isWindows
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.PsiTreeUtil
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.RLanguage
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageInstaller
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.rinterop.RInterop
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

  private val getFirstResult = { str: String -> Regex("\"(.*)\"").find(str)?.groupValues?.get(1) }

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

  private fun getRealPath(rInterop: RInterop, symbol: String, helpPackage: String? = null): String? {

    // .packages(TRUE) != loadNamespaces()
    val searchSpace = if (helpPackage == null) "loadedNamespaces()" else "'$helpPackage'"
    var result = rInterop.findPackagePathByTopic(symbol, searchSpace)

    return if (result.stdout == "character(0)\n" && helpPackage != null) {
      result = rInterop.findPackagePathByPackageName(helpPackage.dropWhile { it == '.' })
      val home = getFirstResult(result.stdout)
      "$home/help/$symbol"
    }
    else {
      getFirstResult(result.stdout)
    }
  }

  private fun getDirNameForConsole(rInterop: RInterop): String? {
    return "R_version_${rInterop.rVersion}"
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

  private fun getHtmlPath(rInterop: RInterop, helpPath: String?): String? {
    helpPath ?: return null

    if (helpPath.startsWith("http")) {
      return helpPath
    }

    val dirNameForConsole = getDirNameForConsole(rInterop) ?: return null

    if (helpPath.endsWith(INDEX_HTML)) {
      val helpPackage = helpPath.split("/")[0]
      val htmlPath = cutPathIfWindows(makePath(pathToDocumentation, dirNameForConsole, helpPackage, INDEX_HTML), WINDOWS_MAX_PATH_LENGTH)

      val htmlFile = File(htmlPath)
      if (!htmlFile.exists()) {
        val result = rInterop.findPackagePathByPackageName(helpPackage)
        val packagePath = getFirstResult(result.stdout)?.replace("/", separator).also {
          if (it == null) {
            LOG.error("Cannot find package $helpPackage. Stdout: ${result.stdout};\n Stderr: ${result.stderr}")
          }
        } ?: return null
        val indexFile = File(makePath(packagePath, "html", INDEX_HTML))
        if (!indexFile.exists()) return null

        val htmlTmpPath = cutPathIfWindows(makePath(pathToDocumentation, dirNameForConsole, helpPackage, "00Index_tmp"),
                                           WINDOWS_MAX_PATH_LENGTH - 1)

        val htmlTmpFile = File(htmlTmpPath)
        try {
          if (!htmlTmpFile.parentFile.exists()) {
            Files.createDirectories(htmlTmpFile.parentFile.toPath())
          }
          Files.copy(indexFile.toPath(), htmlTmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

          if (!convertHelpPage(htmlTmpPath, htmlPath)) {
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
        }
      }
      return htmlPath
    }

    val split = helpPath.split("/")

    val helpPackage = split[split.size - 3]
    val helpPage = split[split.size - 1]

    val htmlPath = cutPathIfWindows(makePath(pathToDocumentation, dirNameForConsole, helpPackage, "$helpPage.html"),
                                    WINDOWS_MAX_PATH_LENGTH)

    val htmlFile = File(htmlPath)
    if (htmlFile.exists()) {
      return htmlPath
    }
    else {
      val htmlTmpPath = cutPathIfWindows(makePath(pathToDocumentation, dirNameForConsole, helpPackage, "${helpPage}_tmp"),
                                         WINDOWS_MAX_PATH_LENGTH - 1)

      val htmlTmpFile = File(htmlTmpPath)

      try {
        if (!htmlTmpFile.parentFile.exists()) {
          Files.createDirectories(htmlFile.parentFile.toPath())
        }

        val result = rInterop.convertRd2HTML(dbPath = helpPath.replace(Regex("/$helpPage$"), "/$helpPackage"),
                                             dbPage = helpPage,
                                             outputFilePath = htmlTmpFile.absolutePath.replace(separator, "/"),
                                             topicPackage = helpPackage)
        if (htmlTmpFile.exists()) {
          if (!convertHelpPage(htmlTmpPath, htmlPath)) {
            LOG.error("Convert help page failed. Rd2HTML stdout: ${result.stdout};\n Rd2HTML stderr: ${result.stderr}")
            return null
          }
          return htmlPath
        }
        else {
          return null
        }
      }
      catch (e: IOException) {
        LOG.error(e)
        return null
      }
      finally {
        htmlTmpFile.delete()
      }
    }
  }

  private fun getHtmlPathForNamespaceAccessExpression(rInterop: RInterop,
                                                      namespaceAccessExpression: RNamespaceAccessExpression): String? {
    val identifier = namespaceAccessExpression.identifier ?: return null
    return getHtmlPath(rInterop, getRealPath(rInterop, identifier.text, namespaceAccessExpression.namespaceName))
  }


  private fun getPath(rInterop: RInterop, psiElement: PsiElement?): String? {

    val element = psiElement ?: return null

    // check if doc of internally rerouted doc-popup click
    restoreInterceptedLink(rInterop, element)?.let { return getHtmlPath(rInterop, it) }

    if (element is RStringLiteralExpression || element is RNamedArgument) {
      return null
    }

    if (element is RHelpExpression) {
      return getHtmlPath(rInterop, getRealPath(rInterop, "help"))
    }

    // qualified name
    if (element is RNamespaceAccessExpression) {
      return getHtmlPathForNamespaceAccessExpression(rInterop, element)
    }

    if (element.parent is RNamespaceAccessExpression) {
      val parent = (element.parent as RNamespaceAccessExpression)
      return getHtmlPathForNamespaceAccessExpression(rInterop, parent)
    }

    val elementFirstChild = element.firstChild
    if (element is RCallExpression && elementFirstChild is RNamespaceAccessExpression) {
      return getHtmlPathForNamespaceAccessExpression(rInterop, elementFirstChild)
    }

    val symbol = when (element) {
      is RAssignmentStatement -> element.name
      is RCallExpression -> elementFirstChild.text
      else -> element.text
    }

    // keyword or function in library
    return getHtmlPath(rInterop, getRealPath(rInterop, symbol))
  }


  override fun generateDoc(psiElement: PsiElement?, identifier: PsiElement?): String? {
    if (psiElement == null || psiElement.language != RLanguage.INSTANCE) return null
    val reference = restoreConsoleHelpCall(psiElement) ?: return null

    checkPossibilityReturnDocumentation(reference) { return it }

    val rInterop = psiElement.containingFile.runtimeInfo?.rInterop ?: return null
    try {
      getDocumentationLocalFunction(rInterop, reference)?.let { path ->
        return Scanner(File(path), StandardCharsets.UTF_8).use {
          it.useDelimiter("\\A").next()
        }
      }
    }
    catch (e: RequiredPackageException) {
      return e.message
    }

    val path = getPath(rInterop, reference) ?: return null

    val inputStream = if (path.startsWith("http")) {
      makeURL(path).openStream()
    }
    else {
      File(path).inputStream()
    }

    return Scanner(inputStream, StandardCharsets.UTF_8).use { it.useDelimiter("\\A").next() }
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
    if (context == null || context.language != RLanguage.INSTANCE) return null
    if (link == INSTALL_REQUIRED_PACKAGES_LINK) {
      RequiredPackageInstaller.getInstance(psiManager.project)
        .installPackagesWithUserPermission(RBundle.message("documentation.utility.name"), localFunctionRequiredPackage, null, false)
      return null
    }

    return RElementFactory.buildRFileFromText(psiManager.project, "help_url(\"${link}\")").firstChild
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

  private fun makeURL(url: String): URL {
    try {
      return URL(url)
    }
    catch (e: MalformedURLException) {
      LOG.error(e)
      throw RuntimeException(e)
    }
  }

  private fun isBlank(str: String?): Boolean {
    return str != null && str.trim { it <= ' ' }.isEmpty()
  }

  private fun restoreInterceptedLink(rInterop: RInterop, reference: PsiElement): String? {
    val localLinkPattern = psiElement(RCallExpression::class.java).withChild(
      psiElement(RIdentifierExpression::class.java).withText("help_url"))

    if (!localLinkPattern.accepts(reference)) {
      return null
    }

    var linkPath = (reference as RCallExpression).argumentList.expressionList[0].text
    linkPath = CharMatcher.anyOf("\"").trimFrom(linkPath)

    if (linkPath.startsWith("http") || linkPath.endsWith(INDEX_HTML)) {
      return linkPath
    }

    val split = linkPath.split("/")
    val helpPackage = split[0]
    val helpPage = split[2].replace(".html", "")

    return getRealPath(rInterop, helpPage, helpPackage)
  }

  private fun restoreConsoleHelpCall(reference: PsiElement?): PsiElement? {
    val consoleHelpCallPattern = psiElement(RCallExpression::class.java).withChild(
      psiElement(RIdentifierExpression::class.java).withText("help_from_console"))

    if (!consoleHelpCallPattern.accepts(reference)) {
      return reference
    }

    val programmeText = (reference as RCallExpression).argumentList.expressionList[0].text.dropWhile { it == '"' }.dropLastWhile { it == '"' }
    return RElementFactory.buildRFileFromText(reference.project, programmeText).firstChild
  }

  private class RequiredPackageException(message: String?) : RuntimeException(message)
}
