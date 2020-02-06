// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages.showInfoMessage
import com.intellij.ui.AnActionButton
import com.intellij.ui.DumbAwareActionButton
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import com.intellij.webcore.packaging.ManagePackagesDialog
import com.intellij.webcore.packaging.PackageManagementService
import com.intellij.webcore.packaging.PackagesNotificationPanel
import com.intellij.webcore.packaging.RepoPackage
import org.jetbrains.r.RBundle
import org.jetbrains.r.UPGRADE_ALL
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RLibraryWatcher
import org.jetbrains.r.packages.RInstalledPackage
import org.jetbrains.r.packages.remote.RPackageManagementService

class RInstalledPackagesPanel(project: Project, area: PackagesNotificationPanel) :
  RInstalledPackagesPanelBase(project, area), RPackageServiceListener {

  private val queue = MergingUpdateQueue(REFRESH_TASK_NAME, REFRESH_TIME_SPAN, true, null, project)
  private val manager: RInterpreterManager = RInterpreterManager.getInstance(project)

  private val listener = object : PackageManagementService.Listener {
    override fun operationStarted(packageName: String?) {
      myPackagesTable.setPaintBusy(true)
    }

    override fun operationFinished(packageName: String?, errorDescription: PackageManagementService.ErrorDescription?) {
      myNotificationArea.showResult(packageName, errorDescription)
      myPackagesTable.clearSelection()
      doUpdatePackages(myPackageManagementService)
    }
  }

  private val isReady: Boolean
    get() = rPackageManagementService != null && manager.interpreter != null && !isTaskRunning

  @Volatile
  private var isTaskRunning = false
  private var rPackageManagementService: RPackageManagementService? = null

  init {
    updateUninstallUpgrade()
    RLibraryWatcher.subscribeAsync(project, RLibraryWatcher.TimeSlot.LAST) {
      scheduleRefresh()
    }
  }

  override fun onTaskStart() {
    isTaskRunning = true
  }

  override fun onTaskFinish() {
    isTaskRunning = false
  }

  override fun getExtraActions(): Array<AnActionButton> {
    return arrayOf(makeUpgradeAllButton(), makeRefreshButton())
  }

  private fun makeUpgradeAllButton(): AnActionButton {
    return object : DumbAwareActionButton(UPGRADE_ALL_TEXT, UPGRADE_ALL_ICON) {
      override fun actionPerformed(e: AnActionEvent) {
        rPackageManagementService?.let { service ->
          val outdated = mutableListOf<RPackageUpdateInfo>()
          val rowCount = myPackagesTable.rowCount
          for (row in 0 until rowCount) {
            val installedPackage = myPackagesTable.getValueAt(row, 0)
            if (installedPackage is RInstalledPackage) {
              val currentVersion = installedPackage.version
              val availableVersion = myPackagesTable.getValueAt(row, 2) as String?
              if (availableVersion != null && availableVersion.isNotBlank() && availableVersion != currentVersion) {
                outdated.add(RPackageUpdateInfo(installedPackage, availableVersion))
              }
            }
          }
          if (outdated.isNotEmpty()) {
            RUpdateAllConfirmDialog(outdated) {
              val packages = outdated.map { RepoPackage(it.installedPackage.name, null, null) }
              val multiListener = RPackageManagementService.convertToInstallMultiListener(listener)
              service.installPackages(packages, true, multiListener)
            }.show()
          } else {
            showInfoMessage(NOTHING_TO_UPGRADE_MESSAGE, NOTHING_TO_UPGRADE_TITLE)
          }
        }
      }

      override fun updateButton(e: AnActionEvent) {
        e.presentation.isEnabled = isReady && rPackageManagementService?.arePackageDetailsLoaded == true
      }
    }
  }

  private fun makeRefreshButton(): AnActionButton {
    return object : DumbAwareActionButton(REFRESH_TEXT, REFRESH_ICON) {
      override fun actionPerformed(e: AnActionEvent) {
        immediatelyUpdatePackages(myPackageManagementService)
      }

      override fun updateButton(e: AnActionEvent) {
        e.presentation.isEnabled = !isTaskRunning
      }
    }
  }

  override fun canUninstallPackage(aPackage: RInstalledPackage): Boolean {
    return isReady && rPackageManagementService?.canUninstallPackage(aPackage) == true
  }

  override fun createManagePackagesDialog(): ManagePackagesDialog {
    val service = rPackageManagementService ?: throw RuntimeException("Cannot open packages dialog: package management service is null")
    return RManagePackagesDialog(myProject, service, listener, this)
  }

  override fun canInstallPackage(aPackage: RInstalledPackage) = isReady

  override fun canUpgradePackage(aPackage: RInstalledPackage?) = isReady

  override fun updatePackages(packageManagementService: RPackageManagementService?) {
    if (rPackageManagementService == null) {
      rPackageManagementService = packageManagementService as RPackageManagementService
      immediatelyUpdatePackages(packageManagementService)
    } else {
      scheduleRefresh()
    }
  }

  private fun immediatelyUpdatePackages(packageManagementService: RPackageManagementService?) {
    super.updatePackages(packageManagementService)
  }

  fun scheduleRefresh() {
    queue.queue(object : Update(REFRESH_TASK_IDENTITY) {
      override fun run() {
        immediatelyUpdatePackages(myPackageManagementService)
      }
    })
  }

  companion object {
    private const val REFRESH_TIME_SPAN = 500
    private val UPGRADE_ALL_TEXT = RBundle.message("packages.panel.upgrade.all.text")
    private val UPGRADE_ALL_ICON = UPGRADE_ALL
    private val NOTHING_TO_UPGRADE_TITLE = RBundle.message("packages.panel.nothing.to.upgrade.title")
    private val NOTHING_TO_UPGRADE_MESSAGE = RBundle.message("packages.panel.nothing.to.upgrade.message")
    private val REFRESH_TEXT = RBundle.message("packages.panel.refresh.text")
    private val REFRESH_ICON = AllIcons.Actions.Refresh
    private val REFRESH_TASK_IDENTITY = RBundle.message("packages.panel.refresh.task.identity")
    private val REFRESH_TASK_NAME = RBundle.message("packages.panel.refresh.task.name")
  }
}
