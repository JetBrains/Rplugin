/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.stubs.classes

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.r.psi.api.RCallExpression

abstract class LibraryClassNameIndexUtil : StringStubIndexExtension<RCallExpression>() {
  companion object{
    fun processClassInfos(project: Project,
                          scope: GlobalSearchScope?,
                          indexKey: StubIndexKey<String, RCallExpression>,
                          indexProcessFunc: RCallExpression.() -> Boolean) {
      val stubIndex = StubIndex.getInstance()
      stubIndex.processAllKeys(indexKey, project) { key ->
        stubIndex.processElements(indexKey, key, project, scope, RCallExpression::class.java) {
          declaration -> indexProcessFunc(declaration)
        }
      }
    }

    fun <T> findClassInfos(indexKey: StubIndexKey<String, RCallExpression>,
                           mapClassInfoFunction: RCallExpression.() -> T?,
                           name: String,
                           project: Project,
                           scope: GlobalSearchScope?): List<T> {
      return StubIndex.getElements(indexKey, name, project, scope, RCallExpression::class.java).mapNotNull { mapClassInfoFunction(it) }
    }

    fun findClassDefinitions(indexKey: StubIndexKey<String, RCallExpression>, name: String, project: Project, scope: GlobalSearchScope?): Collection<RCallExpression> {
      return StubIndex.getElements(indexKey, name, project, scope, RCallExpression::class.java)
    }

    fun sink(indexKey: StubIndexKey<String, RCallExpression>, sink: IndexSink, name: String) {
      sink.occurrence(indexKey, name)
    }
  }
}