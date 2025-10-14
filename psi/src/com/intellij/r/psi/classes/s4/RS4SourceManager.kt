/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.classes.s4

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.r.psi.RLanguage
import com.intellij.r.psi.classes.s4.classInfo.RS4ClassInfo
import com.intellij.r.psi.console.RConsoleRuntimeInfo
import com.intellij.r.psi.psi.api.RCallExpression
import com.intellij.r.psi.psi.api.RFile
import com.intellij.r.psi.rinterop.RInterop
import com.intellij.r.psi.skeleton.psi.RSkeletonCallExpression
import com.intellij.testFramework.ReadOnlyLightVirtualFile
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class RS4SourceManager(private val project: Project) {

  private val files = ConcurrentHashMap<RSkeletonCallExpression, RCallExpression>()
  // TODO: Manage old psi after drop caches
  private val rInteropFiles = ConcurrentHashMap<RConsoleRuntimeInfo, RInterop.Cached<ConcurrentHashMap<String, Optional<RCallExpression>>>>()

  fun getCallFromSkeleton(setClass: RSkeletonCallExpression): RCallExpression {
    return files.getOrPut(setClass) { infoToDeclaration(setClass.stub.s4ClassInfo) }
  }

  fun getSourceCallFromInterop(runtimeInfo: RConsoleRuntimeInfo, className: String): RCallExpression? {
    val files by rInteropFiles.getOrPut(runtimeInfo) {
      Disposer.register(runtimeInfo.rInterop) { rInteropFiles.remove(runtimeInfo) }
      runtimeInfo.rInterop.cached { ConcurrentHashMap<String, Optional<RCallExpression>>() }
    }

    return files.getOrPut(className) {
      Optional.ofNullable(runtimeInfo.loadS4ClassInfoByClassName(className)?.let { info -> infoToDeclaration(info) })
    }.orElse(null)
  }

  fun isS4ClassSourceElement(element: PsiElement): Boolean {
    val file = element.containingFile as? RFile ?: return false
    return file.getUserData(S4_GENERATED_SOURCE_KEY) != null
  }

  private fun infoToDeclaration(classInfo: RS4ClassInfo): RCallExpression {
    val pkg = classInfo.packageName
    val name = classInfo.className
    val virtualFile = ReadOnlyLightVirtualFile("$pkg::setClass('$name')", RLanguage.INSTANCE, classInfo.getDeclarationText(project))
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)!!
    psiFile.putUserData(S4_GENERATED_SOURCE_KEY, Unit)
    return psiFile.firstChild as RCallExpression
  }

  companion object {
    private val S4_GENERATED_SOURCE_KEY: Key<Unit> = Key.create("S4_GENERATED_SOURCE")

    fun isS4ClassSourceElement(element: PsiElement): Boolean = element.withService { isS4ClassSourceElement(element) }
    fun getCallFromSkeleton(setClass: RSkeletonCallExpression): RCallExpression = setClass.withService { getCallFromSkeleton(setClass) }
    fun getSourceCallFromInterop(runtimeInfo: RConsoleRuntimeInfo?, className: String): RCallExpression? =
      runtimeInfo?.rInterop?.project?.withService { getSourceCallFromInterop(runtimeInfo, className) }

    private fun <T> PsiElement.withService(f: RS4SourceManager.() -> T): T = project.withService(f)
    private fun <T> Project.withService(f: RS4SourceManager.() -> T): T = service<RS4SourceManager>().f()
  }
}