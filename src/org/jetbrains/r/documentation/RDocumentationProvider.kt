// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.documentation

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo.isWindows
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.popup.AbstractPopup
import org.jetbrains.annotations.Nls
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
import org.jetbrains.r.RLanguage
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.editor.completion.RLookupElement
import org.jetbrains.r.highlighting.DOC_COMMENT
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageInstaller
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.refactoring.quoteIfNeeded
import org.jetbrains.r.refactoring.rNamesValidator
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RSourceFileManager
import org.jetbrains.r.rinterop.getWithCheckCanceled
import java.awt.Color
import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeoutException
import java.util.function.Consumer
import java.util.function.Supplier

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

  override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): @Nls String? {
    return RQuickNavigateBuilder.getQuickNavigationInfo(element, originalElement)
  }

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
  private fun getHtmlLocalFunction(rInterop: RInterop,
                                   localFunction: RAssignmentStatement,
                                   docStringValue: String?): Supplier<FetchedDoc>? {
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
      DocumentationManagerUtil.createHyperlink(buffer, INSTALL_REQUIRED_PACKAGES_LINK,
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
    return Supplier {
      val result = rInterop.convertRoxygenToHTML(functionName, functionText)
      val docText = if (result.exception != null) {
        LOG.error("RoxygenToHTML. ${result.exception}")
        null
      }
      else {
        convertHelpPage(RInterop.HttpdResponse(result.stdout, "")) +
          "<hr>\n<div style=\"text-align: center;\">[Package <em>${fileName}</em>]</div>\n"
      }
      FetchedDoc(docText)
    }
  }

  @Throws(RequiredPackageException::class)
  private fun getDocumentationLocalFunction(rInterop: RInterop, reference: PsiElement): Supplier<FetchedDoc>? {
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
        return getHtmlLocalFunction(rInterop, localFunction, assignedValue.docStringValue)
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

  override fun generateHoverDoc(element: PsiElement, originalElement: PsiElement?): @Nls String? {
    if (element.language != RLanguage.INSTANCE || element is PsiComment) return null

    // On hover popup appears only if doc exists, we can't do this asynchronously
    return generateDocSynchronously(element, originalElement)
  }

  fun generateDocSynchronously(element: PsiElement, originalElement: PsiElement?): String? {
    return generateDocSupplier(element, originalElement)?.get()?.docText
  }

  override fun generateDoc(psiElement: PsiElement?, identifier: PsiElement?): @Nls String? {
    if (psiElement == null || psiElement.language != RLanguage.INSTANCE || psiElement is PsiComment) return null
    psiElement.getCopyableUserData(ELEMENT_TEXT)?.let {
      psiElement.putCopyableUserData(ELEMENT_TEXT, null)
      return it().docText
    }
    val supplier = generateDocSupplier(psiElement, identifier) ?: return null
    val task = runAsync { supplier.get() }
    try {
      return task.blockingGet(150)?.docText
    }
    catch (e: TimeoutException) {
      task.onSuccess { docText ->
        invokeLater {
          val project = psiElement.project
          if (project.isDisposed) return@invokeLater
          psiElement.putCopyableUserData(ELEMENT_TEXT) { docText }
          val documentationManager = DocumentationManager.getInstance(project)
          val hint = documentationManager.docInfoHint
          val component =
            if (hint != null) (hint as AbstractPopup).component as DocumentationComponent
            else RToolWindowFactory.getDocumentationComponent(project)
          documentationManager.queueFetchDocInfo(psiElement, component)
        }
      }
      return CodeInsightBundle.message("javadoc.fetching.progress")
    }
  }

  private fun generateDocSupplier(psiElement: PsiElement, identifier: PsiElement?): Supplier<FetchedDoc>? {
    val rInterop = psiElement.containingFile.runtimeInfo?.rInterop ?: return null
    if (psiElement is RDocumentationFakeTargetElement) {
      val url = psiElement.name.orEmpty()
      return Supplier {
        val docText = if (url.startsWith("http")) {
          val stream = URL(url).openStream()
          Scanner(stream, StandardCharsets.UTF_8).use { it.useDelimiter("\\A").next() }
        }
        else {
          rInterop.httpdRequest(url)?.let { adjustHtmlDocumentation(convertHelpPage(it)) }
        }
        FetchedDoc(docText)
      }
    }
    checkPossibilityReturnDocumentation(psiElement) { return Supplier { FetchedDoc(it) } }
    val element = PsiTreeUtil.getNonStrictParentOfType(psiElement, RPsiElement::class.java) ?: return null
    try {
      getDocumentationLocalFunction(rInterop, element)?.let { return it }
    } catch (e: RequiredPackageException) {
      return Supplier { FetchedDoc(e.message) }
    }

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
    return Supplier {
      val docText = rInterop.getDocumentationForSymbol(symbol, pkg).getWithCheckCanceled()?.let { convertHelpPage(it) }
      FetchedDoc(docText)
    }
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
    val stringLiteral = RElementFactory.createRPsiElementFromText(psiManager.project, "\"${StringUtil.escapeStringCharacters(link)}\"")
    return RDocumentationFakeTargetElement(stringLiteral as RStringLiteralExpression)
  }

  override fun getDocumentationElementForLookupItem(psiManager: PsiManager?, item: Any?, element: PsiElement?): PsiElement? {
    if (psiManager == null || item !is RLookupElement) return null
    val name = item.lookup
    val pkg = item.packageName
    val project = psiManager.project
    val quotedName = rNamesValidator.quoteIfNeeded(name, project)

    val text = if (pkg == null)
      quotedName
    else
      "${rNamesValidator.quoteIfNeeded(pkg, project)}::$quotedName"

    return RElementFactory.createRPsiElementFromText(project, text)
  }

  override fun collectDocComments(file: PsiFile, sink: Consumer<in PsiDocCommentBase>) {
    val virtualFile = file.virtualFile
    if (virtualFile == null || !RSourceFileManager.isTemporary(virtualFile)) {
      return
    }
    val first = file.children.firstOrNull()
    if (first !is PsiComment || !first.text.matches(DOCUMENTATION_COMMENT_REGEX)) {
      return
    }
    val docComment = RVirtualDocumentationComment(file, first.textRange)
    sink.accept(docComment)
  }

  override fun generateRenderedDoc(comment: PsiDocCommentBase): @Nls String? {
    val containingFile = comment.containingFile
    if (containingFile !is RFile) {
      return null
    }
    val rInterop = containingFile.runtimeInfo?.rInterop
    if (rInterop != null) {
      val methodNameAndPackage = getMethodNameAndPackage(comment.text)
      val documentationPromise = rInterop.getDocumentationForSymbol(methodNameAndPackage.first, methodNameAndPackage.second)
      val documentationResponse = documentationPromise.get()
      if (documentationResponse != null) {
        val htmlDocumentation = StringBuilder(convertHelpPage(documentationResponse))
        adjustHtmlDocumentationForEditor(htmlDocumentation)
        return htmlDocumentation.toString()
      }
    }

    return null
  }

  override fun findDocComment(file: PsiFile, range: TextRange): PsiDocCommentBase? {
    val comment = PsiTreeUtil.getParentOfType(file.findElementAt(range.startOffset), PsiComment::class.java, false)
    return if (comment == null || range != comment.textRange) null else RVirtualDocumentationComment(file, range)
  }

  private class RequiredPackageException(message: String?) : RuntimeException(message)

  companion object {
    private const val PACKAGE_METHOD_SEPARATOR = "::"
    private const val BACKTICK = '`'
    private data class FetchedDoc(val docText: String?)
    private val ELEMENT_TEXT = Key<() -> FetchedDoc>("org.jetbrains.r.documentation.ElementText")
    private val DOCUMENTATION_COMMENT_REGEX = "^# `?.+`?::`?.+`?$".toRegex()

    private fun getMethodNameAndPackage(documentationComment: String): Pair<String, String?> {
      val text = documentationComment.substring(PACKAGE_METHOD_SEPARATOR.length)
      val commaSeparatorIndex = text.indexOf(PACKAGE_METHOD_SEPARATOR)
      if (commaSeparatorIndex < 0) {
        return Pair(text, null)
      }
      val packageAndMethod = text.split(PACKAGE_METHOD_SEPARATOR.toRegex(), 2).map { s -> StringUtil.unquoteString(s, BACKTICK) }
      return Pair(packageAndMethod[1], packageAndMethod[0])
    }

    private fun adjustHtmlDocumentation(documentation: String) : String {
      val noWrapCss = """
        <style>
          td code {
            white-space: nowrap;
          }        
        </style>
        """.trimIndent()
      return noWrapCss + documentation
    }

    private fun adjustHtmlDocumentationForEditor(documentation: StringBuilder) {
      // remove the horizontal line at the bottom and centered text [Package <em><package-name></em> version]
      val horizontalLineIndex = documentation.lastIndexOf("<hr")
      if (horizontalLineIndex > 0) {
        documentation.delete(horizontalLineIndex, documentation.length)
      }

      val documentationColor = getHexFromColor(DOC_COMMENT.defaultAttributes.foregroundColor)
      val noWrapCss = """
        <style>
          td code {
            white-space: nowrap;
          }        
        </style>
        """.trimIndent()
      documentation.insert(0, "${noWrapCss}\n<div style=\"color:${documentationColor};padding:5px\">")
      documentation.insert(documentation.length, "</div>")
    }

    private fun getHexFromColor(color: Color): String {
      return String.format("#%02x%02x%02x", color.red, color.green, color.blue)
    }

    fun makeElementForText(rInterop: RInterop, httpdResponse: RInterop.HttpdResponse): PsiElement {
      return RElementFactory.createRPsiElementFromText(rInterop.project, "text").also {
        it.putCopyableUserData(ELEMENT_TEXT) { FetchedDoc(convertHelpPage(httpdResponse)) }
      }
    }

    internal fun convertHelpPage(httpdResponse: RInterop.HttpdResponse): String {
      var (text, url) = httpdResponse
      val bodyStart = text.indexOf("<body>")
      val bodyEnd = text.lastIndexOf("</body>")
      if (bodyStart != -1 && bodyEnd != -1) text = text.substring(bodyStart + "<body>".length, bodyEnd)
      val urlComponents = url.substringBefore('?').substringAfter("://")
        .substringAfter('/', "").substringBeforeLast('/', "")
        .split('/')
      return Regex("href\\s*=\\s*\"([^\"]*)\"").replace(text) {
        val link = it.groupValues[1]
        val result = when {
          link.startsWith('#') -> "psi_element://$url"
          link.startsWith("http://127.0.0.1") -> {
            "psi_element:///" + link.substringAfter("://").substringAfter('/', "")
          }
          "://" in link -> link
          link.startsWith('/') -> "psi_element://$link"
          else -> {
            var parentCount = 0
            while (link.startsWith("../", parentCount * 3)) ++parentCount
            val prefix = urlComponents.dropLast(parentCount).joinToString("/")
            "psi_element:///" + prefix + (if (prefix.isEmpty()) "" else "/") + link.drop(parentCount * 3)
          }
        }
        "href=\"$result\""
      }
        .let { Regex("<a href.*<img.*></a>").replace(it, "") }
        .let { Regex("<img.*>").replace(it, "") }
    }
  }
}
