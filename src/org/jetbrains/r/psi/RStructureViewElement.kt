/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.TextRange
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.templateLanguages.OuterLanguageElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.ArrayUtil
import com.intellij.util.PlatformIcons
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RFile
import org.jetbrains.r.psi.api.RFunctionExpression
import org.jetbrains.r.psi.api.RIdentifierExpression
import java.util.*
import javax.swing.Icon

class RStructureViewElement(private val element: PsiElement) : StructureViewTreeElement, ItemPresentation {
  override fun getValue(): Any {
    return element
  }

  override fun navigate(requestFocus: Boolean) {
    (element as Navigatable).navigate(requestFocus)
  }

  override fun canNavigate(): Boolean {
    return (element as Navigatable).canNavigate()
  }

  override fun canNavigateToSource(): Boolean {
    return (element as Navigatable).canNavigateToSource()
  }

  override fun getPresentation(): ItemPresentation {
    return this
  }

  override fun getChildren(): Array<StructureViewTreeElement> {
    val childrenElements = ArrayList<StructureViewTreeElement>()

    val functionCollector = RStructureVisitor(childrenElements)

    if (element is RFile) {
      element.acceptChildren(object : PsiElementVisitor() {
        override fun visitComment(comment: PsiComment) {
          if (RPsiUtil.isSectionDivider(comment)) {
            childrenElements.add(RStructureViewElement(comment))
          }
        }

        override fun visitElement(e: PsiElement) {
          // don't add function on root level if we have
          if (childrenElements.any { it.value is PsiComment }) return

          e.accept(functionCollector)
        }
        //no further recursion here because we just support sectioning on top level
      })
    }

    if (RPsiUtil.isSectionDivider(element)) {
      var nextSibling: PsiElement? = element.nextSibling
      while (nextSibling != null && !RPsiUtil.isSectionDivider(nextSibling)) {
        nextSibling.accept(functionCollector)
        nextSibling = nextSibling.nextSibling
      }
    }

    return ArrayUtil.toObjectArray(childrenElements, StructureViewTreeElement::class.java)
  }

  override fun getPresentableText(): String? {
    return when {
      element is RFile -> element.name
      element is RAssignmentStatement && element.assignedValue is RFunctionExpression ->
        element.assignee?.text ?: "anonymous function"
      element is RAssignmentStatement ->
        element.assignee?.text ?: throw IllegalStateException("Empty name")
      element is PsiComment -> RPsiUtil.extractNameFromSectionComment(element)
      else -> throw IllegalStateException("Unknown structure node: ${element.javaClass.name}")
    }
  }

  override fun getLocationString(): String? {
    return null
  }

  override fun getIcon(open: Boolean): Icon? {
    return when {
      element is RAssignmentStatement && element.assignedValue is RFunctionExpression -> PlatformIcons.FUNCTION_ICON
      element is RAssignmentStatement -> PlatformIcons.VARIABLE_ICON
      open -> IconLoader.getIcon("/nodes/folderOpen.png")
      else -> PlatformIcons.FOLDER_ICON
    }
  }

  class RStructureVisitor(private val result: MutableList<StructureViewTreeElement>,
                          private val textRange: TextRange? = null) : RRecursiveElementVisitor() {
    override fun visitAssignmentStatement(assignment: RAssignmentStatement) {
      super.visitAssignmentStatement(assignment)

      if (assignment.assignee?.text.isNullOrEmpty()) return

      if (assignment.assignedValue is RFunctionExpression || firstGlobalVariableDefinition(assignment)) {
        result.add(RStructureViewElement(assignment))
      }
    }

    override fun visitFunctionExpression(o: RFunctionExpression) {
      // Do nothing, just stop recursion
    }

    override fun visitElement(element: PsiElement) {
      if (textRange != null && !textRange.intersects(element.textRange)) return
      super.visitElement(element)
    }
  }
}

private data class NamespaceSegment(val start: Int, val nameMap: Map<String, PsiElement>)

private fun firstGlobalVariableDefinition(assignment: RAssignmentStatement): Boolean {
  val identifier = assignment.assignee as? RIdentifierExpression ?: return false
  val ranges: List<NamespaceSegment> = getOrCalculateVariableMap(identifier.containingFile)

  val comparator = Comparator<NamespaceSegment> { o1: NamespaceSegment, o2 ->
    o1.start.compareTo(o2.start)
  }

  val searchMarker = NamespaceSegment(assignment.textRange.startOffset, emptyMap())
  val beforeIndex = ranges.binarySearch(searchMarker, comparator).let {
    if (it >= 0) it else -it - 2
  }
  assert(beforeIndex >= 0) { "Error in the index calculation, ${beforeIndex}" }

  return ranges[beforeIndex].nameMap[identifier.text] == assignment
}

private fun getOrCalculateVariableMap(file: PsiFile): List<NamespaceSegment> {
  return CachedValuesManager.getCachedValue(file) l@ {
    val result: MutableList<NamespaceSegment> = ArrayList<NamespaceSegment>()
    var nameMap = HashMap<String, PsiElement>()
    result.add(NamespaceSegment(0, nameMap))
    object: RRecursiveElementVisitor() {
      override fun visitAssignmentStatement(assignment: RAssignmentStatement) {
        (assignment.assignee as? RIdentifierExpression)?.let {
          nameMap.putIfAbsent(it.text, assignment)
        }
        super.visitAssignmentStatement(assignment)
      }

      override fun visitFunctionExpression(o: RFunctionExpression) {
        // Do nothing, just stop recursion
      }

      override fun visitElement(element: PsiElement) {
        if (element is OuterLanguageElement && nameMap.isNotEmpty()) {
          nameMap = HashMap<String, PsiElement>()
          result.add(NamespaceSegment(element.textRange.endOffset, nameMap))
        }
        super.visitElement(element)
      }
    }.visitElement(file)

    return@l CachedValueProvider.Result(result, file)
  }
}