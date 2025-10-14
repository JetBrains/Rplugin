package com.intellij.r.psi.console

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.intellij.r.psi.classes.r6.R6ClassInfo
import com.intellij.r.psi.classes.s4.classInfo.RS4ClassInfo
import com.intellij.r.psi.hints.parameterInfo.RExtraNamedArgumentsInfo
import com.intellij.r.psi.psi.TableInfo
import com.intellij.r.psi.psi.api.RFunctionExpression
import com.intellij.r.psi.rinterop.RInterop
import com.intellij.r.psi.rinterop.RInteropManager
import com.intellij.r.psi.rinterop.RValue
import org.jetbrains.annotations.TestOnly

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
  fun loadR6ClassInfoByObjectName(objectName: String): R6ClassInfo?
  fun getFormalArguments(expression: String) : List<String>
  fun loadTableColumns(expression: String): TableInfo
  val rInterop: RInterop

  companion object {
    fun get(psiFile: PsiFile): RConsoleRuntimeInfo? {
      if (ApplicationManager.getApplication().isUnitTestMode) psiFile.getUserData(KEY)?.let { return it }

      return get(psiFile.project)
    }

    fun get(project: Project): RConsoleRuntimeInfo? {
      val rInterop = RInteropManager.getInstance(project).currentConsoleInterop() ?: return null
      return rInterop.consoleRuntimeInfo.takeIf { rInterop.isAlive }
    }
  }
}

@TestOnly
private val KEY: Key<RConsoleRuntimeInfo> = Key.create("com.intellij.r.psi.console.runtimeInfo")

val PsiFile.runtimeInfo: RConsoleRuntimeInfo?
  get() = RConsoleRuntimeInfo.get(this)
