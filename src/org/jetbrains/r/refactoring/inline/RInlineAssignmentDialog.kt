/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.inline

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.refactoring.inline.InlineOptionsDialog
import com.intellij.usageView.UsageViewNodeTextLocation
import org.jetbrains.r.RBundle
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RIdentifierExpression

class RInlineAssignmentDialog(project: Project,
                              private val editor: Editor?,
                              private val isFunction: Boolean,
                              private val assignment: RAssignmentStatement,
                              private val refElement: RIdentifierExpression?) : InlineOptionsDialog(project, true, assignment) {
  init {
    myInvokedOnReference = refElement != null
    title =
      if (isFunction) RBundle.message("inline.assignment.dialog.function.title")
      else RBundle.message("inline.assignment.dialog.variable.title")
    init()
  }

  override fun getNameLabelText(): String {
    return RBundle.message("inline.assignment.dialog.name.label",
                           if (isFunction) {
                             assignment.text.takeWhile { it != '{' }.trim()
                           }
                           else ElementDescriptionUtil.getElementDescription(myElement, UsageViewNodeTextLocation.INSTANCE))
  }

  override fun getBorderTitle(): String = RBundle.message("inline.diaglog.title.inline")
  override fun getKeepTheDeclarationText(): String? = if (isFunction) RBundle.message("inline.assignment.dialog.keep.text") else null
  override fun getInlineThisText(): String? = RBundle.message("inline.assignment.dialog.only.this.text")
  override fun getInlineAllText(): String? = RBundle.message("inline.assignment.dialog.all.text")
  override fun isInlineThis(): Boolean = isFunction

  override fun doAction() {
    invokeRefactoring(
      RInlineAssignmentProcessor(project, editor, assignment, refElement, isInlineThisOnly, isFunction, !isKeepTheDeclaration))
  }

  override fun doHelpAction() {}
}
