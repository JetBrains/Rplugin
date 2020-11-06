/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.TestOnly
import org.jetbrains.r.classes.s4.RS4ClassInfo
import org.jetbrains.r.hints.parameterInfo.RExtraNamedArgumentsInfo
import org.jetbrains.r.psi.TableInfo
import org.jetbrains.r.psi.api.RFunctionExpression
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RReference
import org.jetbrains.r.rinterop.RValue
import org.jetbrains.r.rinterop.getWithCheckCanceled
import java.util.concurrent.atomic.AtomicReference

interface RConsoleRuntimeInfo {
  val variables: Map<String, RValue>
  val loadedPackages: Map<String, Int>
  val rMarkdownChunkOptions: List<String>
  val workingDir: String
  fun loadPackage(name: String)
  fun loadDistinctStrings(expression: String): List<String>
  fun loadObjectNames(expression: String) : List<String>
  fun loadInheritorNamedArguments(baseFunctionName: String) : List<String>
  fun loadExtraNamedArguments(functionName: String): RExtraNamedArgumentsInfo
  fun loadExtraNamedArguments(functionName: String, functionExpression: RFunctionExpression): RExtraNamedArgumentsInfo
  fun loadShortS4ClassInfos(): List<RS4ClassInfo>
  fun loadS4ClassInfoByObjectName(objectName: String): RS4ClassInfo?
  fun loadS4ClassInfoByClassName(className: String): RS4ClassInfo?
  fun getFormalArguments(expression: String) : List<String>
  fun loadTableColumns(expression: String): TableInfo
  val rInterop: RInterop

  companion object {
    fun get(psiFile: PsiFile): RConsoleRuntimeInfo? {
      if (ApplicationManager.getApplication().isUnitTestMode) psiFile.getUserData(KEY)?.let { return it }

      return get(psiFile.project)
    }

    fun get(project: Project): RConsoleRuntimeInfo? {
      val console = RConsoleManager.getInstance(project).currentConsoleOrNull ?: return null
      return console.consoleRuntimeInfo.takeIf { console.rInterop.isAlive }
    }
  }
}

class RConsoleRuntimeInfoImpl(override val rInterop: RInterop) : RConsoleRuntimeInfo {
  private val objectNamesCache by rInterop.Cached { mutableMapOf<String, List<String>>() }
  private val distinctStringsCache by rInterop.Cached { mutableMapOf<String, List<String>>() }
  private val inheritorNamedArgumentsCache by rInterop.Cached { mutableMapOf<String, List<String>>() }
  private val extraNamedArgumentsCache by rInterop.Cached { mutableMapOf<String, RExtraNamedArgumentsInfo>() }
  private val extraNamedArgumentsStampCache by rInterop.Cached { mutableMapOf<String, Long>() }
  private val formalArgumentsCache by rInterop.Cached { mutableMapOf<String, List<String>>() }
  private val tableColumnsCache by rInterop.Cached { mutableMapOf<String, TableInfo>() }
  private val s4ClassInfosByObjectNameCache by rInterop.Cached { mutableMapOf<String, RS4ClassInfo?>() }
  private val s4ClassInfosByClassNameCache by rInterop.Cached { mutableMapOf<String, RS4ClassInfo?>() }
  private val loadedShortS4ClassInfosCache by rInterop.Cached { AtomicReference<List<RS4ClassInfo>?>(null) }

  override val rMarkdownChunkOptions by lazy { rInterop.rMarkdownChunkOptions }

  override val variables
    get() = rInterop.currentEnvLoader.variables.map { it.name to it.value }.toMap()
  override val loadedPackages
    get() = rInterop.loadedPackages.getWithCheckCancel()
  override val workingDir
    get() = rInterop.workingDir

  override fun loadPackage(name: String) {
    rInterop.loadLibrary(name).getWithCheckCanceled()
  }

  override fun loadDistinctStrings(expression: String): List<String> {
    return distinctStringsCache.getOrPut(expression) { RReference.expressionRef(expression, rInterop).getDistinctStrings() }
  }

  override fun loadObjectNames(expression: String): List<String> {
    return objectNamesCache.getOrPut(expression) { RReference.expressionRef(expression, rInterop).ls() }
  }

  override fun loadInheritorNamedArguments(baseFunctionName: String): List<String> {
    return inheritorNamedArgumentsCache.getOrPut(baseFunctionName) {
      rInterop.findInheritorNamedArguments(RReference.expressionRef("'$baseFunctionName'", rInterop))
    }
  }

  override fun loadExtraNamedArguments(functionName: String): RExtraNamedArgumentsInfo {
    return extraNamedArgumentsCache.getOrPut(functionName) {
      rInterop.findExtraNamedArguments(RReference.expressionRef("'$functionName'", rInterop))
    }
  }

  @Synchronized
  override fun loadExtraNamedArguments(functionName: String, functionExpression: RFunctionExpression): RExtraNamedArgumentsInfo {
    val stamp = functionExpression.containingFile.modificationStamp
    val oldStamp = extraNamedArgumentsStampCache[functionName]
    if (oldStamp == null || stamp < 0 || stamp != oldStamp) {
      extraNamedArgumentsStampCache[functionName] = stamp
      return rInterop.findExtraNamedArguments(RReference.expressionRef("'${functionExpression.text}'", rInterop)).also {
        extraNamedArgumentsCache[functionName] = it
      }
    }
    return extraNamedArgumentsCache.getValue(functionName)
  }

  /**
   * @return list of [RS4ClassInfo] without information about [RS4ClassInfo.slots] and [RS4ClassInfo.superClasses]
   */
  override fun loadShortS4ClassInfos(): List<RS4ClassInfo> {
    loadedShortS4ClassInfosCache.get().let { infos ->
      if (infos != null) return infos
      return rInterop.getLoadedShortS4ClassInfos().also {
        loadedShortS4ClassInfosCache.set(it)
      } ?: emptyList()
    }
  }

  override fun loadS4ClassInfoByObjectName(objectName: String): RS4ClassInfo? {
    return s4ClassInfosByObjectNameCache.getOrPut(objectName) {
      rInterop.getS4ClassInfoByObjectName(RReference.expressionRef(objectName, rInterop))
    }
  }

  override fun loadS4ClassInfoByClassName(className: String): RS4ClassInfo? {
    return s4ClassInfosByClassNameCache.getOrPut(className) {
      rInterop.getS4ClassInfoByClassName(className)
    }
  }

  override fun getFormalArguments(expression: String): List<String> {
    return formalArgumentsCache.getOrPut(expression) { rInterop.getFormalArguments(RReference.expressionRef(expression, rInterop)) }
  }

  override fun loadTableColumns(expression: String): TableInfo {
    return tableColumnsCache.getOrPut(expression) {
      rInterop.getTableColumnsInfo(RReference.expressionRef(expression, rInterop))
    }
  }
}

@TestOnly
private val KEY: Key<RConsoleRuntimeInfo> = Key.create("org.jetbrains.r.console.runtimeInfo")

@TestOnly
internal fun PsiFile.addRuntimeInfo(runtimeInfo: RConsoleRuntimeInfo) {
  putUserData(KEY, runtimeInfo)
}

val PsiFile.runtimeInfo: RConsoleRuntimeInfo?
  get() = RConsoleRuntimeInfo.get(this)
