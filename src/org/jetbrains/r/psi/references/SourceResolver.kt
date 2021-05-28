/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.references

import com.intellij.codeInsight.controlflow.Instruction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.RecursionManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.ex.temp.TempFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.packages.RSkeletonUtil
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.findVariableDefinition
import org.jetbrains.r.psi.isFunctionFromLibrarySoft
import org.jetbrains.r.util.RPathUtil
import java.nio.file.Path

sealed class IncludedSources {

  /**
   * @return true if all of [element]'s possible definitions are in sourced files, false otherwise
   */
  fun resolveInSources(element: RPsiElement,
                       name: String,
                       result: MutableList<ResolveResult>,
                       localResolveResult: PsiElement?): Boolean {
    return resolveInSources(element, name, result, localResolveResult, SourceResolverCache())
  }

  protected fun resolveInSources(element: RPsiElement,
                                 name: String,
                                 result: MutableList<ResolveResult>,
                                 localResolveResult: PsiElement?,
                                 cachedValues: SourceResolverCache): Boolean {
    ProgressManager.checkCanceled()
    val lastConsideredSource = (localResolveResult as? RPsiElement?)?.let {
      val controlFlowHolder = PsiTreeUtil.getParentOfType(it, RControlFlowHolder::class.java)
      controlFlowHolder?.getIncludedSources(it)
    }
    val innerResult = mutableListOf<ResolveResult>()
    return resolveInSourcesInnerWithStopChecks(element, name, innerResult, lastConsideredSource, cachedValues).also {
      result.addAll(innerResult.distinct())
    }
  }

  protected fun resolveInSourcesInnerWithStopChecks(element: RPsiElement,
                                                    name: String,
                                                    result: MutableList<ResolveResult>,
                                                    lastConsideredSource: IncludedSources?,
                                                    cachedValues: SourceResolverCache): Boolean {
    ProgressManager.checkCanceled()
    if (this == lastConsideredSource) return false
    return cachedValues.resolveWithCache(this) {
      resolveInSourcesInner(element, name, result, lastConsideredSource, cachedValues)
    }
  }

  /**
   * This function shouldn't make general checks.
   * It is assumed that when calling it, you need to look at the internal structure to make a conclusion about the resolve result.
   * Most probably you don't want to call this function directly.
   * @see [resolveInSourcesInnerWithStopChecks]
   */
  protected abstract fun resolveInSourcesInner(element: RPsiElement,
                                               name: String,
                                               result: MutableList<ResolveResult>,
                                               lastConsideredSource: IncludedSources?,
                                               cachedValues: SourceResolverCache): Boolean

  class SingleSource(private val project: Project,
                     private val filename: String,
                     private val prev: List<IncludedSources> = emptyList()) : IncludedSources() {
    private var file: RFile? = null
      @Synchronized
      get() =
        if (field == null || !field!!.isValid) findRFile(filename, project).also { field = it }
        else field

    private fun findRFile(filename: String, project: Project): RFile? {
      val relativePath = RPathUtil.toPath(filename) ?: return null
      val unitTestMode = ApplicationManager.getApplication().isUnitTestMode
      val root = if (unitTestMode) "/src" else project.basePath ?: return null
      val path = RPathUtil.toPath(root)?.resolve(relativePath) ?: return null
      val virtualFile = if (unitTestMode) findTempFile(path) else VfsUtil.findFile(path, true)
      return PsiManager.getInstance(project).findFile(virtualFile ?: return null) as? RFile
    }

    override fun resolveInSourcesInner(element: RPsiElement,
                                       name: String,
                                       result: MutableList<ResolveResult>,
                                       lastConsideredSource: IncludedSources?,
                                       cachedValues: SourceResolverCache): Boolean {
      val file = file
      if (file != null) {
        val ret = cachedValues.resolveWithCache(file) {
          val virtualFile = file.virtualFile
          val tmpResult = mutableListOf<ResolveResult>()
          RResolver.resolveInFile(element, name, tmpResult, virtualFile)
          val resolveResult = tmpResult.singleOrNull()
          val resolveElement = resolveResult?.element
          RecursionManager.doPreventingRecursion(file, true) {
            // Analysis of local file statements doesn't make sense in other files
            if (file.getAllIncludedSources().resolveInSources(element, name, result, resolveElement, cachedValues.fileCacheCopy())) {
              return@doPreventingRecursion true
            }
            if (resolveResult != null) {
              result.add(resolveResult)
              return@doPreventingRecursion true
            }
            return@doPreventingRecursion false
          } ?: false
        }
        if (ret) return true
      }
      return prev.map { it.resolveInSourcesInnerWithStopChecks(element, name, result, lastConsideredSource, cachedValues) }.all()
    }
  }

  class MultiSource(private val multiSources: IncludedSources? = null,
                    private val prev: List<IncludedSources> = emptyList()) : IncludedSources() {
    override fun resolveInSourcesInner(element: RPsiElement,
                                       name: String,
                                       result: MutableList<ResolveResult>,
                                       lastConsideredSource: IncludedSources?,
                                       cachedValues: SourceResolverCache): Boolean {
      return if (multiSources?.resolveInSourcesInnerWithStopChecks(element, name, result, lastConsideredSource, cachedValues) != true) {
        prev.map { it.resolveInSourcesInnerWithStopChecks(element, name, result, lastConsideredSource, cachedValues) }.all()
      }
      else true
    }
  }

  protected class SourceResolverCache(val sourcesCache: MutableMap<IncludedSources, Boolean> = mutableMapOf(),
                                      val fileCache: MutableMap<RFile, Boolean> = mutableMapOf()) {

    inline fun resolveWithCache(includedSource: IncludedSources, resolveAction: () -> Boolean): Boolean {
      return sourcesCache.getOrPut(includedSource, resolveAction)
    }

    inline fun resolveWithCache(rFile: RFile, resolveAction: () -> Boolean): Boolean {
      return fileCache.getOrPut(rFile, resolveAction)
    }

    fun fileCacheCopy(): SourceResolverCache = SourceResolverCache(fileCache = fileCache)
  }
}

private fun List<Boolean>.all(): Boolean {
  return if (isEmpty()) false
  else this.all { it }
}

private val EMPTY = IncludedSources.MultiSource()

private fun RControlFlowHolder.getAllIncludedSources() = includedSources.getOrDefault(controlFlow.instructions.last(), EMPTY)

fun RControlFlowHolder.analyseIncludedSources(): Map<Instruction, IncludedSources> {
  return RecursionManager.doPreventingRecursion(this, true) { analyseIncludedSourcesInner() } ?: emptyMap()
}

private fun RControlFlowHolder.analyseIncludedSourcesInner(): Map<Instruction, IncludedSources> {
  ProgressManager.checkCanceled()
  val result = mutableMapOf<Instruction, IncludedSources>()
  result[controlFlow.instructions[0]] = EMPTY
  for (instruction in controlFlow.instructions.drop(1)) {
    val prevSources = instruction.allPred()
      .filter { pred -> controlFlow.isReachable(pred) && pred.num() < instruction.num() }
      .distinct()
      .map { updateSources(result.getValue(it), it.element) }
    // Distinct to rid extra EMPTY values
    result[instruction] = prevSources.distinct().let { it.singleOrNull() ?: IncludedSources.MultiSource(prev = it) }
  }

  return result
}

private fun getSourceDeclaration(sourceIdentifier: PsiElement): RAssignmentStatement? {
  val result = mutableListOf<ResolveResult>()
  RResolver.resolveInFilesOrLibrary(sourceIdentifier, "source", result)
  return result.mapNotNull { it.element }.firstOrNull {
    val file = it.containingFile
    val (name, _) = RSkeletonUtil.skeletonFileToRPackage(file) ?: return@firstOrNull false
    name == "base"
  } as? RAssignmentStatement
}

private fun updateSources(sources: IncludedSources, element: PsiElement?): IncludedSources {
  ProgressManager.checkCanceled()
  if (element !is RCallExpression) return sources
  val localDefinition = (element.expression as? RIdentifierExpression)?.findVariableDefinition()?.variableDescription?.firstDefinition
  return if (localDefinition == null && element.isFunctionFromLibrarySoft("source", "base")) {
    val filepathArgument = RArgumentInfo.getArgumentByName(element, "file", getSourceDeclaration(element.expression))
    val filepath = if (filepathArgument is RStringLiteralExpression) filepathArgument.name else null
    if (filepath != null) IncludedSources.SingleSource(element.project, filepath, listOf(sources))
    else sources
  }
  else {
    val function = (localDefinition?.parent as? RAssignmentStatement)?.assignedValue as? RFunctionExpression ?: return sources
    function.getAllIncludedSources().let {
      if (it != EMPTY) IncludedSources.MultiSource(it, listOf(sources))
      else sources
    }
  }
}

private fun findTempFile(filePath: Path): VirtualFile? {
  val absoluteFilePath = filePath.toString()
  val fileSystem = TempFileSystem.getInstance()
  var virtualFile = fileSystem.findFileByPath(absoluteFilePath)
  if (virtualFile == null || !virtualFile.isValid) {
    virtualFile = fileSystem.refreshAndFindFileByPath(absoluteFilePath)
  }
  return virtualFile
}