// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote

import com.intellij.execution.ExecutionException
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.webcore.packaging.PackagesNotificationPanel
import com.intellij.webcore.packaging.RepoPackage
import org.jetbrains.r.RBundle
import org.jetbrains.r.interpreter.RLibraryWatcher
import org.jetbrains.r.packages.RInstalledPackage
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RInteropTerminated
import java.util.concurrent.ConcurrentSkipListSet

class RPackageTaskManager(
  private val rInterop: RInterop?,
  private val project: Project,
  private val listener: TaskListener
) {

  private val packagesInProgress: ConcurrentSkipListSet<String>
    get() = project.getUserData(KEY) ?: ConcurrentSkipListSet<String>().also { project.putUserData(KEY, it) }

  fun install(packages: List<RepoPackage>, repoUrls: List<String>) {
    val installPackages = packages.filter { !packagesInProgress.contains(it.name) }
    packagesInProgress.addAll(installPackages.map { it.name })
    runTask(RBundle.message("package.task.manager.install.title"), installPackages.map { InstallTaskAction(rInterop, project, it, repoUrls) })
  }

  fun update(packages: List<RepoPackage>, repoUrls: List<String>) {
    val updatePackages = packages.filter { !packagesInProgress.contains(it.name) }
    packagesInProgress.addAll(updatePackages.map { it.name })
    runTask(RBundle.message("package.task.manager.update.title"), updatePackages.map { UpdateTaskAction(rInterop, project, it, repoUrls) })
  }

  fun uninstall(packages: List<RInstalledPackage>) {
    runTask(RBundle.message("package.task.manager.uninstall.title"), listOf(UninstallTaskAction(rInterop, project, packages)))
  }

  private fun runTask(title: String, actions: List<PackagingTaskAction>) {
    ProgressManager.getInstance().run(PackagingTask(project, title, listener, actions))
  }

  interface TaskListener {
    fun started()
    fun finished(exceptions: List<ExecutionException?>)
  }

  private class PackagingTask(
    project: Project,
    title: String,
    private val listener: TaskListener,
    private val actions: List<PackagingTaskAction>
  ) : Task.Backgroundable(project, title) {

    override fun run(indicator: ProgressIndicator) {
      val watcher = RLibraryWatcher.getInstance(project)
      watcher.disable()
      try {
        runTask(indicator)
      } finally {
        watcher.enable()
      }
    }

    private fun runTask(indicator: ProgressIndicator) {
      taskStarted(indicator)
      val exceptions = mutableListOf<ExecutionException?>()
      for ((index, action) in actions.withIndex()) {
        try {
          indicator.text = makeIndicatorText(index)
          indicator.fraction = index.toDouble() / actions.count().toDouble()
          action.doAction()
          exceptions.add(null)
          taskNotify(action, null)
        }
        catch (e: ExecutionException) {
          exceptions.add(e)
          taskNotify(action, e)
        }
        catch (e: RInteropTerminated) {
          val exception = ExecutionException(RBundle.message("rinterop.terminated"))
          exceptions.add(exception)
          taskNotify(action, exception)
        }
      }
      taskFinished(exceptions)
    }

    private fun makeIndicatorText(index: Int): String {
      val action = actions[index]
      return if (actions.size > 1) {
        "Packaging task $index/${actions.size}: ${action.name}"
      } else {
        action.name
      }
    }

    private fun taskStarted(indicator: ProgressIndicator) {
      indicator.isIndeterminate = false
      ApplicationManager.getApplication().invokeLater {
        listener.started()
      }
    }

    private fun taskNotify(action: PackagingTaskAction, exception: ExecutionException?) {
      val notification = if (exception != null) {
        RPackageManagementService.toErrorDescription(exception)?.let { description ->
          val listener = NotificationListener { _, _ ->
            val title = StringUtil.capitalizeWords(action.failureTitle, true)
            PackagesNotificationPanel.showError(title, description)
          }
          val content = exception.message ?: RBundle.message("notification.content.check.console.output.for.details")
          Notification(PACKAGING_GROUP_ID, action.failureTitle, content, NotificationType.ERROR, listener)
        }
      }
      else {
        Notification(PACKAGING_GROUP_ID, action.successTitle, action.successDescription, NotificationType.INFORMATION, null)
      }
      notification?.notify(myProject)
    }

    private fun taskFinished(exceptions: List<ExecutionException?>) {
      val notification = if (actions.count() > 1) {
        val numExceptions = exceptions.count { it != null }
        if (numExceptions == 0) {
          val title = RBundle.message("package.task.manager.finished.success.title", title)
          val content = RBundle.message("package.task.manager.finished.success.content", actions.count())
          Notification(PACKAGING_GROUP_ID, title, content, NotificationType.INFORMATION, null)
        }
        else {
          val title = RBundle.message("package.task.manager.finished.failed.title", title)
          val content = RBundle.message("package.task.manager.finished.failed.content", numExceptions, actions.size - numExceptions)
          Notification(PACKAGING_GROUP_ID, title, content, NotificationType.ERROR, null)
        }
      }
      else {
        null
      }
      notification?.notify(myProject)
      ApplicationManager.getApplication().invokeLater {
        listener.finished(exceptions)
      }
    }

    companion object {
      private val PACKAGING_GROUP_ID = RBundle.message("package.task.manager.notification.group.id")
    }
  }

  private interface PackagingTaskAction {
    val name: String
    val successTitle: String
    val successDescription: String
    val failureTitle: String
    val failureDescription: String
    fun doAction()
  }

  private class InstallTaskAction(
    private val rInterop: RInterop?,
    private val project: Project,
    private val repoPackage: RepoPackage,
    private val repoUrls: List<String>
  ) : PackagingTaskAction {

    override val name = RBundle.message("install.task.action.name", repoPackage.name)
    override val successTitle = RBundle.message("install.task.action.success.title")
    override val successDescription = RBundle.message("install.task.action.success.description", repoPackage.name)
    override val failureTitle = RBundle.message("install.task.action.failure.title")
    override val failureDescription = RBundle.message("install.task.action.failure.description", repoPackage.name)

    override fun doAction() {
      try {
        RepoUtils.installPackage(rInterop, project, repoPackage, repoUrls)
      }
      finally {
        project.getUserData(KEY)!!.remove(repoPackage.name)
      }
    }
  }

  private class UpdateTaskAction(
    private val rInterop: RInterop?,
    private val project: Project,
    private val repoPackage: RepoPackage,
    private val repoUrls: List<String>
  ) : PackagingTaskAction {

    override val name = RBundle.message("update.task.action.name", repoPackage.name)
    override val successTitle = RBundle.message("update.task.action.success.title")
    override val successDescription = RBundle.message("update.task.action.success.description", repoPackage.name)
    override val failureTitle = RBundle.message("update.task.action.failure.title")
    override val failureDescription = RBundle.message("update.task.action.failure.description", repoPackage.name)

    override fun doAction() {
      try {
        RepoUtils.updatePackage(rInterop, project, repoPackage, repoUrls)
      }
      finally {
        project.getUserData(KEY)!!.remove(repoPackage.name)
      }
    }
  }

  private class UninstallTaskAction(
    private val rInterop: RInterop?,
    private val project: Project,
    private val packages: List<RInstalledPackage>
  ) : PackagingTaskAction {

    override val name = RBundle.message("uninstall.task.action.name")
    override val successTitle = RBundle.message("uninstall.task.action.success.title")
    override val successDescription = RBundle.message("uninstall.task.action.success.description", packages.joinToString(", ") { it.name })
    override val failureTitle = RBundle.message("uninstall.task.action.failure.title")
    override val failureDescription = RBundle.message("uninstall.task.action.failure.description", packages.joinToString(", ") { it.name })

    override fun doAction() {
      packages.forEach { RepoUtils.uninstallPackage(rInterop, project, it) }
    }
  }

  companion object {
    private val KEY: Key<ConcurrentSkipListSet<String>> = Key.create("PACKAGES_IN_INSTALLATION_OR_UPDATING")
  }
}
