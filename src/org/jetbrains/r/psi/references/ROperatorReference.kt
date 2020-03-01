// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.references

import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveResult
import com.intellij.testFramework.ReadOnlyLightVirtualFile
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.RInfixOperator
import org.jetbrains.r.psi.api.ROperator
import java.util.*

class ROperatorReference(element: ROperator) : RReferenceBase<ROperator>(element) {

  override fun multiResolveInner(incompleteCode: Boolean): Array<ResolveResult> {
    val result = ArrayList<ResolveResult>()

    val text = element.text
    //        String doubleQuotedText = "\"" + text + "\"";
    //        String singleQuotedText = "'" + text + "'";
    //        String backquotedText = "`" + text + "`";

    RResolver.resolveInFilesOrLibrary(element, text, result)
    return result.toTypedArray()
  }

  override fun handleElementRename(newElementName: String): PsiElement? {
    if (element is RInfixOperator) {
      return (element as RInfixOperator).setName(newElementName)
    }
    return null
  }

  override fun getVariants(): Array<Any> {
    return emptyArray()
  }

}
