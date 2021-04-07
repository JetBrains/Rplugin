/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.stubs.classes

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processor
import org.jetbrains.r.classes.r6.R6ClassInfo
import org.jetbrains.r.psi.api.RCallExpression

class R6ClassNameIndex : LibraryClassNameIndex() {
  companion object {
    private val KEY = StubIndexKey.createIndexKey<String, RCallExpression>("R.r6class.shortName")

    fun processAllClassInfos(project: Project, scope: GlobalSearchScope?, processor: Processor<Pair<RCallExpression, R6ClassInfo>>) {
      val stubIndex = StubIndex.getInstance()
      stubIndex.processAllKeys(KEY, project) { key ->
        stubIndex.processElements(KEY, key, project, scope, RCallExpression::class.java) { declaration ->
          declaration.associatedR6ClassInfo?.let { processor.process(declaration to it) } ?: true
        }
      }
    }

    fun findClassInfos(name: String, project: Project, scope: GlobalSearchScope?): List<R6ClassInfo> {
      return StubIndex.getElements(KEY, name, project, scope, RCallExpression::class.java).mapNotNull { it.associatedR6ClassInfo }
    }
  }
}