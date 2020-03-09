// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.psi.references

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.console.RConsoleRuntimeInfo
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.interpreter.RInterpreterManager.Companion.getInstance
import org.jetbrains.r.packages.build.RPackageBuildUtil
import org.jetbrains.r.psi.RPomTarget
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.RPsiUtil.getFunction
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.stubs.RAssignmentNameIndex
import org.jetbrains.r.skeleton.psi.RSkeletonAssignmentStatement
import java.util.*
import java.util.function.Predicate

object RResolver {
  internal val LOG = Logger.getInstance(
    "#" + RResolver::class.java.name)

  fun resolveWithNamespace(project: Project,
                           name: String,
                           namespace: String,
                           result: MutableList<ResolveResult>) {
    val interpreter = getInstance(project).interpreter ?: return
    val psiFile = interpreter.getSkeletonFileByPackageName(namespace) ?: return
    val statements = RAssignmentNameIndex.find(name, project, GlobalSearchScope.fileScope(psiFile))
    for (statement in statements) {
      if (statement.name == name) {
        result.add(PsiElementResolveResult(statement))
      }
    }
  }

  fun <T> not(t: Predicate<T>): Predicate<T> {
    return t.negate()
  }

  fun resolveNameArgument(element: PsiElement,
                          elementName: String,
                          result: MutableList<ResolveResult>) {
    val callExpression = PsiTreeUtil.getParentOfType(element, RCallExpression::class.java)
    if (callExpression != null) {
      val functionExpression = getFunction(callExpression)
      val parameterList = PsiTreeUtil.getChildOfType(functionExpression, RParameterList::class.java)
      if (parameterList != null) {
        for (parameter in parameterList.parameterList) {
          if (parameter.name == elementName) {
            result.add(0, PsiElementResolveResult(parameter))
            return
          }
        }
      }
    }
  }

  private fun resolveBase(element: PsiElement,
                          name: String,
                          result: MutableList<ResolveResult>,
                          globalSearchScope: GlobalSearchScope) {
    val statements = RAssignmentNameIndex.find(name, element.project, globalSearchScope)
    val exported = statements.filter { it !is RSkeletonAssignmentStatement || it.stub.exported }
    addResolveResults(result, exported)
  }

  fun resolveInFilesOrLibrary(element: PsiElement,
                              name: String,
                              result: MutableList<ResolveResult>) {
    resolveBase(element, name, result, RSearchScopeUtil.getScope(element))
  }

  fun resolveInFile(element: PsiElement,
                    name: String,
                    result: MutableList<ResolveResult>,
                    file: VirtualFile) {
    resolveBase(element, name, result, GlobalSearchScope.fileScope(element.project, file))
  }

  fun resolveUsingSourcesAndRuntime(element: RPsiElement, name: String, localResolveResult: ResolveResult?):  Array<ResolveResult> {
    val result = ArrayList<ResolveResult>()
    val controlFlowHolder = PsiTreeUtil.getParentOfType(element, RControlFlowHolder::class.java)
    if (controlFlowHolder?.getIncludedSources(element)?.resolveInSources(element, name, result, localResolveResult?.element) != true) {
      if (localResolveResult != null) {
        if (localResolveResult !is EmptyLocalResult) result.add(localResolveResult)
        if (result.isEmpty()) return emptyArray()
      }
      else {
        addSortedResultsInFilesOrLibrary(element, name, result)
      }
    }

    if (result.isNotEmpty()) {
      val distinct = result.distinct()
      return if (distinct.size > 1) {
        distinct.map { PsiElementResolveResult(it.element!!, false) }.toTypedArray()
      }
      else distinct.toTypedArray()
    }

    element.containingFile.runtimeInfo?.let { consoleRuntimeInfo ->
      val variables = consoleRuntimeInfo.rInterop.globalEnvLoader.variables
      variables.firstOrNull { it.name == name }?.let {
        return arrayOf(PsiElementResolveResult(RPomTarget.createPsiElementByRValue(it)))
      }
    }

    resolveInFilesOrLibrary(element, name, result)
    return result.toTypedArray()
  }

  object EmptyLocalResult : ResolveResult {
    override fun getElement(): PsiElement? = null
    override fun isValidResult(): Boolean = false
  }

  private fun addResolveResults(result: MutableList<ResolveResult>,
                                statements: Collection<RAssignmentStatement>) {
    for (statement in statements) {
      result.add(PsiElementResolveResult(statement))
    }
  }

  private fun getLoadingNumber(loadedNamespaces: Map<String, Int>, result: ResolveResult): Int {
    val name = RReferenceBase.findPackageNameByResolveResult(result) ?: return Int.MAX_VALUE
    return loadedNamespaces[name] ?: Int.MAX_VALUE
  }

  fun sortResolveResults(psiElement: PsiElement,
                         runtimeInfo: RConsoleRuntimeInfo?,
                         resolveResults: Array<ResolveResult>): Array<ResolveResult> {
    resolveResults.firstOrNull { it.element?.containingFile == psiElement.containingFile }?.let { return arrayOf(it) }
    val fileUnderProject = psiElement.containingFile.virtualFile?.let {
      GlobalSearchScope.projectScope(psiElement.project).contains(it)
    } ?: false
    if (fileUnderProject && RPackageBuildUtil.isPackage(psiElement.project)) {
      resolveResults.filterNot { RPsiUtil.isLibraryElement(it.element ?: return@filterNot true) }.toTypedArray().let {
        if (it.isNotEmpty()) return it
      }
    }
    if (runtimeInfo != null) {
      val loadedPackages = runtimeInfo.loadedPackages
      val topResolveResult = resolveResults.minBy { getLoadingNumber(loadedPackages, it) } ?: return resolveResults
      if (getLoadingNumber(loadedPackages, topResolveResult) != Int.MAX_VALUE) {
        return arrayOf(topResolveResult)
      }
    }
    return resolveResults
  }

  fun addSortedResultsInFilesOrLibrary(element: RPsiElement, name: String, result: MutableList<ResolveResult>) {
    val libraryOrFileResult = mutableListOf<ResolveResult>()
    resolveInFilesOrLibrary(element, name, libraryOrFileResult)
    result.addAll(sortResolveResults(element, element.containingFile.runtimeInfo, libraryOrFileResult.toTypedArray()))
  }
}