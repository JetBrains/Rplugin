package org.jetbrains.r.codeInsight.libraries

import com.intellij.psi.*
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
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

    return resolveInputMembers(element, uiDefinition, serverDefinition)
  }

  private fun resolveInputMembers(element: RPsiElement,
                                  uiDefinition: RAssignmentStatement,
                                  serverDefinition: RAssignmentStatement): ResolveResult? {
    if (!PsiTreeUtil.isAncestor(serverDefinition, element, true)) {
      return null
    }
    val parent = element.parent
    if (parent !is RMemberExpression) {
      return null
    }
    val callableObject = parent.leftExpr
    if (callableObject !is RIdentifierExpression || callableObject.text != INPUT_OBJECT) {
      return null
    }

    var result: ResolveResult? = null
    processInputElements(uiDefinition, object : Processor<PsiElement> {
      override fun process(namedUiElement: PsiElement?): Boolean {
        if (namedUiElement is RStringLiteralExpressionImpl) {
          if (namedUiElement.name == element.name) {
            result = PsiElementResolveResult(namedUiElement)
            return false
          }
        }
        return true
      }

    })
    return result
  }

  /**
   * Processes input elements defined in Shiny's "ui" assignment
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
      CachedValueProvider.Result.create(retrieveNamedElements(uiDefinition), uiDefinition)
    }
  }

  /**
   * Retrieves all named elements defined in "ui" assignment.
   *
   * The code below defines element with name "num"
   * ```
   * ui <- fluidPage(
   *   fluidRow(
   *     column(5, sliderInput(inputId = "num")
   *   )
   * )
   * ```
   */
  private fun retrieveNamedElements(uiDefinition: RAssignmentStatement): List<SmartPsiElementPointer<PsiElement>> {
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

  companion object {
    const val SERVER_VARIABLE = "server"
    const val UI_VARIABLE = "ui"
    const val INPUT_ID_ATTRIBUTE = "inputId"
    const val INPUT_OBJECT = "input"
  }
}