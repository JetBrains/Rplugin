package org.jetbrains.r.codeInsight.libraries

import com.intellij.psi.*
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import org.jetbrains.annotations.NonNls
import org.jetbrains.r.psi.RRecursiveElementVisitor
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.impl.RStringLiteralExpressionImpl

class RShinySupportProvider : RLibrarySupportProvider {
  override fun resolve(element: RPsiElement): ResolveResult? {
    val uiAndServerElements = getUiAndServerElements(element)

    val uiDefinition = uiAndServerElements.first
    val serverDefinition = uiAndServerElements.second
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
    if (callableObject.text == INPUT_OBJECT) {
      processInputElements(uiDefinition, resolveProcessor)
    }
    if (callableObject.text == OUTPUT_OBJECT) {
      processOutputElements(uiDefinition, resolveProcessor)
    }
    return resolveProcessor.result
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
        for (namedArgument in call.argumentList.namedArgumentList) {
          val assignedValue = namedArgument.assignedValue
          if (namedArgument.name == INPUT_ID_ATTRIBUTE && assignedValue is RStringLiteralExpression) {
            val elementName = assignedValue.name
            if (elementName != null) {
              result.add(SmartPointerManager.createPointer(assignedValue as PsiElement))
            }
          }
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
        if (call.expression is RIdentifierExpression && call.expression.text.endsWith(OUTPUT_CALL_SUFFIX)) {
          if (call.argumentList.expressionList.isNotEmpty()) {
            val outputIdCandidate = call.argumentList.expressionList.first()
            if (outputIdCandidate is RStringLiteralExpression) {
              result.add(SmartPointerManager.createPointer(outputIdCandidate))
            }
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
          if (assignee.text == UI_VARIABLE) {
            uiDefinition = child
          }
          if (assignee.text == SERVER_VARIABLE) {
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

  class ShinyResolveProcessor(private var elementName: String) : Processor<PsiElement> {
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

  companion object {
    @NonNls const val SERVER_VARIABLE = "server"
    @NonNls const val UI_VARIABLE = "ui"
    @NonNls const val INPUT_ID_ATTRIBUTE = "inputId"
    @NonNls const val INPUT_OBJECT = "input"
    @NonNls const val OUTPUT_OBJECT = "output"
    @NonNls const val OUTPUT_CALL_SUFFIX = "Output"
  }
}