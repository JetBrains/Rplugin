/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.stubs.classes

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.Processor
import org.jetbrains.r.psi.api.RCallExpression

abstract class LibraryClassNameIndex<T> : StringStubIndexExtension<RCallExpression>() {

  protected abstract val mapClassInfoFunction: RCallExpression.() -> T?

  fun processAllClassInfos(project: Project,
                             scope: GlobalSearchScope?,
                             processor: Processor<Pair<RCallExpression, T>>) {
    val stubIndex = StubIndex.getInstance()
    stubIndex.getAllKeys(key, project).forEach { key ->
      stubIndex.processElements(getKey(), key, project, scope, RCallExpression::class.java) { declaration ->
        declaration.mapClassInfoFunction()?.let { processor.process(declaration to it) } ?: true
      }
    }
  }

  fun findClassInfos(name: String, project: Project, scope: GlobalSearchScope?): List<T> {
    return StubIndex.getElements(key, name, project, scope, RCallExpression::class.java).mapNotNull { mapClassInfoFunction(it) }
  }

  fun findClassDefinitions(name: String, project: Project, scope: GlobalSearchScope?): Collection<RCallExpression> {
    return StubIndex.getElements(key, name, project, scope, RCallExpression::class.java)
  }

  fun sink(sink: IndexSink, name: String) {
    sink.occurrence(key, name)
  }
}