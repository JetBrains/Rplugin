package org.jetbrains.r.codeInsight.libraries

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.psi.*
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import org.jetbrains.annotations.NonNls
import org.jetbrains.r.editor.completion.RLookupElementFactory
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil
import org.jetbrains.r.psi.RRecursiveElementVisitor
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.impl.RStringLiteralExpressionImpl

class RShinySupportProvider : RLibrarySupportProvider {
  override fun resolve(element: RPsiElement): ResolveResult? {
    val (uiDefinition, serverDefinition) = getUiAndServerElements(element)

    if (uiDefinition == null || serverDefinition == null) {
      return null
    }

    val elementName = element.name
    if (elementName == null) {
      return null
    }
    if (!PsiTreeUtil.isAncestor(serverDefinition, element, true)) {
      return null
    }
    val parent = element.parent
    if (parent !is RMemberExpression) {
      return null
    }
    val callableObject = parent.leftExpr
    if (callableObject !is RIdentifierExpression) {
      return null
    }
    val resolveProcessor = ShinyResolveProcessor(elementName)
    if (callableObject.name == INPUT_OBJECT) {
      processInputElements(uiDefinition, resolveProcessor)
    }
    if (callableObject.name == OUTPUT_OBJECT) {
      processOutputElements(uiDefinition, resolveProcessor)
    }
    return resolveProcessor.result
  }

  override fun completeMembers(receiver: RPsiElement,
                               lookupElementFactory: RLookupElementFactory,
                               completionConsumer: CompletionResultSet) {
    if (receiver !is RIdentifierExpression || (receiver.name != INPUT_OBJECT && receiver.name != OUTPUT_OBJECT)) {
      return
    }
    val uiAndServerElements = getUiAndServerElements(receiver)
    val uiDefinition = uiAndServerElements.first
    val serverDefinition = uiAndServerElements.second

    if (uiDefinition == null || serverDefinition == null || !PsiTreeUtil.isAncestor(serverDefinition, receiver, true)) {
      return
    }

    val completionProcessor = ShinyCompletionProcessor(lookupElementFactory, completionConsumer)
    if (receiver.name == INPUT_OBJECT) {
      processInputElements(uiDefinition, completionProcessor)
    }
    if (receiver.name == OUTPUT_OBJECT) {
      processOutputElements(uiDefinition, completionProcessor)
    }
  }

  override fun completeIdentifier(element: PsiElement,
                                  lookupElementFactory: RLookupElementFactory,
                                  completionConsumer: CompletionResultSet) {
    completeHtmlTagAttributes(element, lookupElementFactory, completionConsumer)
  }

  private fun completeHtmlTagAttributes(element: PsiElement,
                                        lookupElementFactory: RLookupElementFactory,
                                        completionConsumer: CompletionResultSet) {
    val ident = PsiTreeUtil.getParentOfType(element, RIdentifierExpression::class.java)
    val call = ident?.parent?.parent // consider case: ident => argument list => call expression
    if (call !is RCallExpression) {
      return
    }

    val uiAndServerElements = getUiAndServerElements(call)
    val uiDefinition = uiAndServerElements.first
    if (uiDefinition == null || !PsiTreeUtil.isAncestor(uiDefinition, element, true)) {
      return
    }

    val receiver = call.expression
    val tagName: String
    if (receiver is RMemberExpression) {
      val tagsObject = receiver.leftExpr
      if (tagsObject !is RIdentifierExpression || tagsObject.name != "tags") {
        return
      }
      val memberElement = receiver.rightExpr
      if (memberElement !is RIdentifierExpression) {
        return
      }
      tagName = memberElement.name
    } else if (receiver is RIdentifierExpression && receiver.name in SHINY_TAG_METHODS) {
      tagName = receiver.name
    } else {
      return
    }

    addCompletionForTag(tagName, lookupElementFactory, completionConsumer)
  }

  private fun addCompletionForTag(tagName: String, lookupElementFactory: RLookupElementFactory, completionConsumer: CompletionResultSet) {
    completionConsumer.consume(lookupElementFactory.createNamedArgumentLookupElement("style"))
    completionConsumer.consume(lookupElementFactory.createNamedArgumentLookupElement("id"))
    completionConsumer.consume(lookupElementFactory.createNamedArgumentLookupElement("class"))

    val customAttributes = SHINY_TAGS_ATTRIBUTES[tagName]
    if (customAttributes != null) {
      for (customAttribute in customAttributes) {
        completionConsumer.consume(lookupElementFactory.createNamedArgumentLookupElement(customAttribute))
      }
    }
  }

  /**
   * Processes input elements defined in Shiny's "ui" assignment.
   * The code below defines element with name "num"
   * ```
   * ui <- fluidPage(
   *   fluidRow(
   *     column(5, sliderInput(inputId = "num")
   *   )
   * )
   * ```
   */
  private fun processInputElements(uiDefinition: RAssignmentStatement,
                                   processor: Processor<PsiElement>): Boolean {

    val inputElements = getInputElements(uiDefinition)

    for (inputElement in inputElements) {
      val resolvedElement = inputElement.element
      if (resolvedElement != null && !processor.process(resolvedElement)) {
        return false
      }
    }

    return true
  }

  private fun getInputElements(uiDefinition: RAssignmentStatement): List<SmartPsiElementPointer<PsiElement>> {
    return CachedValuesManager.getCachedValue(uiDefinition) {
      CachedValueProvider.Result.create(retrieveInputElements(uiDefinition), uiDefinition)
    }
  }

  private fun retrieveInputElements(uiDefinition: RAssignmentStatement): List<SmartPsiElementPointer<PsiElement>> {
    val result = ArrayList<SmartPsiElementPointer<PsiElement>>()

    uiDefinition.accept(object : RRecursiveElementVisitor() {
      override fun visitCallExpression(call: RCallExpression) {
        val inputIdArgument = RParameterInfoUtil.getArgumentByName(call, INPUT_ID_ATTRIBUTE)
        if (inputIdArgument != null) {
          result.add(SmartPointerManager.createPointer(inputIdArgument as PsiElement))
        }
        super.visitCallExpression(call)
      }
    })
    return result
  }

  /**
   * Processes output elements defined in Shiny's "ui" assignment
   * According to the Shiny documentation it's calls inside assignment to the "ui" variable
   * that ended with "Output" suffix
   *
   * ```
   * ui <- fluidPage(
   *   ...
   *   plotOutput("hist")
   *   ...
   * )
   * ```
   */
  private fun processOutputElements(uiDefinition: RAssignmentStatement,
                                    processor: Processor<PsiElement>): Boolean {

    val outputElements = getOutputElements(uiDefinition)

    for (outputElement in outputElements) {
      val resolvedElement = outputElement.element
      if (resolvedElement != null && !processor.process(resolvedElement)) {
        return false
      }
    }

    return true
  }

  private fun getOutputElements(uiDefinition: RAssignmentStatement): List<SmartPsiElementPointer<PsiElement>> {
    return CachedValuesManager.getCachedValue(uiDefinition) {
      CachedValueProvider.Result.create(retrieveOutputElements(uiDefinition), uiDefinition)
    }
  }

  private fun retrieveOutputElements(uiDefinition: RAssignmentStatement): List<SmartPsiElementPointer<PsiElement>> {
    val result = ArrayList<SmartPsiElementPointer<PsiElement>>()

    uiDefinition.accept(object : RRecursiveElementVisitor() {
      override fun visitCallExpression(call: RCallExpression) {
        val callExpression = call.expression
        if (callExpression is RIdentifierExpression && callExpression.name.endsWith(OUTPUT_CALL_SUFFIX)) {
          val outputIdArgument = RParameterInfoUtil.getArgumentByName(call, OUTPUT_ID_ATTRIBUTE)
          if (outputIdArgument != null) {
            result.add(SmartPointerManager.createPointer(outputIdArgument))
          }
        }
        super.visitCallExpression(call)
      }
    })
    return result
  }

  /**
   * Retrieves assignments to variables "ui" and "sever".
   *
   * "ui" defines layout of the page. Here should be created all elements.
   * "server" defines mapping and interactions between ui and data.
   */
  private fun getUiAndServerElements(element: RPsiElement): Pair<RAssignmentStatement?, RAssignmentStatement?> {
    val file = element.containingFile
    if (file !is RFile) {
      return Pair<RAssignmentStatement?, RAssignmentStatement?>(null, null)
    }

    var uiDefinition: RAssignmentStatement? = null
    var serverDefinition: RAssignmentStatement? = null
    for (child in file.children) {
      if (child is RAssignmentStatement) {
        val assignee = child.assignee
        if (assignee is RIdentifierExpression) {
          if (assignee.name == UI_VARIABLE) {
            uiDefinition = child
          }
          if (assignee.name == SERVER_VARIABLE) {
            serverDefinition = child
          }
        }
      }
      if (uiDefinition != null && serverDefinition != null) {
        break
      }
    }

    return Pair(uiDefinition, serverDefinition)
  }

  private class ShinyResolveProcessor(private var elementName: String) : Processor<PsiElement> {
    var result: PsiElementResolveResult? = null

    override fun process(namedUiElement: PsiElement?): Boolean {
      if (namedUiElement is RStringLiteralExpressionImpl) {
        if (namedUiElement.name == this.elementName) {
          result = PsiElementResolveResult(namedUiElement)
          return false
        }
      }
      return true
    }
  }

  private class ShinyCompletionProcessor(private var lookupElementFactory: RLookupElementFactory,
                                         private var completionConsumer: CompletionResultSet) : Processor<PsiElement> {
    override fun process(namedUiElement: PsiElement?): Boolean {
      if (namedUiElement is RStringLiteralExpressionImpl) {
        val elementName = namedUiElement.name
        if (!elementName.isNullOrEmpty()) {
          completionConsumer.consume(lookupElementFactory.createMemberLookupElement(elementName, priority = CUSTOM_ATTRIBUTE_PRIORITY))
        }
      }
      return true
    }
  }

  companion object {
    @NonNls
    const val SERVER_VARIABLE = "server"
    @NonNls
    const val UI_VARIABLE = "ui"
    @NonNls
    const val INPUT_ID_ATTRIBUTE = "inputId"
    @NonNls
    const val OUTPUT_ID_ATTRIBUTE = "outputId"
    @NonNls
    const val INPUT_OBJECT = "input"
    @NonNls
    const val OUTPUT_OBJECT = "output"
    @NonNls
    const val OUTPUT_CALL_SUFFIX = "Output"

    const val CUSTOM_ATTRIBUTE_PRIORITY = 200.0

    /**
     * Custom attributes according to @see [list of tags](https://shiny.rstudio.com/articles/tag-glossary.html)
     */
    @NonNls
    val SHINY_TAGS_ATTRIBUTES = mapOf(
      "a" to listOf("href"),
      "audio" to listOf("autoplay", "controls", "src", "type"),
      "blockquote" to listOf("cite"),
      "embed" to listOf( "src", "type", "height", "width"),
      "iframe" to listOf( "src", "srcdoc", "scrolling", "seamless", "height", "width", "name"),
      "img" to listOf( "src", "height", "width"),
      "video" to listOf("autoplay", "controls", "src", "height", "width"))

    @NonNls
    val SHINY_TAG_METHODS = listOf("p", "h1", "h2", "h3", "h4", "h5", "h6", "a", "br", "div", "span", "pre", "code", "img", "strong", "em",
                                   "hr")

    @NonNls
    val SHINY_TAG_CONTAINERS = listOf("absolutePanel", "fixedPanel", "bootstrapPage", "column", "conditionalPanel", "fillPage", "fillRow",
                                      "fixedPage", "fluidPage", "helpText", "navbarPage", "navlistPanel", "sidebarLayout", "tabPanel",
                                      "tabsetPanel", "titlePanel", "inputPanel", "flowLayout", "splitLayout", "verticalLayout", "wellPanel",
                                      "withMathJax")
  }
}