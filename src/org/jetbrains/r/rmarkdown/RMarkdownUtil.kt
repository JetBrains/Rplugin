/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownParagraph
import org.jetbrains.annotations.Nls
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.await
import org.jetbrains.r.RBundle
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageInstaller
import kotlin.Throws

object RMarkdownUtil {
  private val requiredPackages = listOf(
    RequiredPackage("rmarkdown", "1.16"),  // Note: minimal version of "1.16" fixes "Extension ascii_identifiers is not supported for markdown" error
    RequiredPackage("knitr")
  )

  fun areRequirementsSatisfied(project: Project): Boolean {
    return getMissingPackages(project)?.isEmpty() == true
  }

  /**
   * @return list of missing R Markdown's requirements or `null` if such a list cannot be formed right now
   * (notably when [interpreter][org.jetbrains.r.interpreter.RInterpreter] hasn't been initialized yet)
   */
  fun getMissingPackages(project: Project): List<RequiredPackage>? {
    return RequiredPackageInstaller.getInstance(project).getMissingPackagesOrNull(requiredPackages)
  }

  @Throws(Throwable::class)
  suspend fun checkOrInstallPackages(project: Project, utilityName: @Nls String) {
    checkOrInstallPackagesAsync(project, utilityName).await()
  }

  @Deprecated("use checkOrInstallPackages instead")
  fun checkOrInstallPackagesAsync(project: Project, utilityName: @Nls String): Promise<Unit> {
    return RequiredPackageInstaller.getInstance(project).installPackagesWithUserPermission(utilityName, requiredPackages)
      .onError {
        notifyFailure(project, utilityName)
      }
  }

  private fun notifyFailure(project: Project, utilityName: @Nls String) {
    val title = makeNotificationTitle(utilityName)
    val content = makeNotificationContent(utilityName)
    Notification(RBundle.message("rmarkdown.processor.notification.group.display"), title, content, NotificationType.ERROR).notify(project)
  }

  private fun makeNotificationTitle(utilityName: @Nls String): @Nls String {
    return RBundle.message("rmarkdown.rendering.packages.notification.title", utilityName)
  }

  private fun makeNotificationContent(utilityName: @Nls String): @Nls String {
    return RBundle.message("rmarkdown.rendering.packages.notification.content", utilityName)
  }

  fun isShiny(file: PsiFile): Boolean = runReadAction {
    val paragraph = findMarkdownParagraph(file) ?: return@runReadAction false
    if (paragraph.text.lines().any { RUNTIME_SHINY_REGEX.matches(it) }) {
      return@runReadAction true
    }
    false
  }

  fun findMarkdownParagraph(file: PsiFile): MarkdownParagraph? =
    SyntaxTraverser.psiTraverser(file).firstOrNull { it is MarkdownParagraph } as MarkdownParagraph?

  val IS_SHINY = Key<Boolean>("org.jetbrains.r.rmarkdown.IsShiny")
  private val RUNTIME_SHINY_REGEX = Regex("\\s*runtime\\s*:\\s*shiny.*")
}
