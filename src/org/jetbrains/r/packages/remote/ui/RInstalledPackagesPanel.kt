// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages.showInfoMessage
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.AnActionButton
import com.intellij.ui.DumbAwareActionButton
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import com.intellij.webcore.packaging.*
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.packages.remote.ui.RPackageUpdateInfo
import icons.org.jetbrains.r.packages.remote.ui.RUpdateAllConfirmDialog
import org.jetbrains.r.RIcons
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RLibraryWatcher
import org.jetbrains.r.packages.remote.RPackageManagementService
import org.jetbrains.r.packages.remote.RepoUtils

class RInstalledPackagesPanel(project: Project, area: PackagesNotificationPanel) :
  InstalledPackagesPanel(project, area), RPackageServiceListener {

  private val queue = MergingUpdateQueue(REFRESH_TASK_NAME, REFRESH_TIME_SPAN, true, null, project)
  private val manager: RInterpreterManager = RInterpreterManager.getInstance(project)
  private lateinit var rPackageManagementService: RPackageManagementService

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

  @Volatile
  private var isTaskRunning = false

  private lateinit var upgradeAllButton: DumbAwareActionButton

  init {
    updateUninstallUpgrade()
    RLibraryWatcher.subscribeAsync(project, RLibraryWatcher.TimeSlot.LATE) {
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
    upgradeAllButton = object : DumbAwareActionButton(UPGRADE_ALL_TEXT, UPGRADE_ALL_ICON) {
      override fun actionPerformed(e: AnActionEvent) {
        val outdated = mutableListOf<RPackageUpdateInfo>()
        val rowCount = myPackagesTable.rowCount
        for (row in 0 until rowCount) {
          val installedPackage = myPackagesTable.getValueAt(row, 0)
          if (installedPackage is InstalledPackage) {
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
            rPackageManagementService.installPackages(packages, true, listener)
          }.show()
        } else {
          showInfoMessage("All packages are already up-to-date", "Nothing to upgrade")
        }
      }

      override fun updateButton(e: AnActionEvent) {
        e.presentation.isEnabled = !isTaskRunning && RepoUtils.getPackageDetails(myProject) != null  // Wait until package details are loaded
      }
    }

    val refreshButton = object : DumbAwareActionButton(REFRESH_TEXT, REFRESH_ICON) {
      override fun actionPerformed(e: AnActionEvent) {
        RepoUtils.resetPackageDescriptions()
        RepoUtils.resetPackageDetails(myProject)
        immediatelyUpdatePackages(myPackageManagementService)
      }

      override fun updateButton(e: AnActionEvent) {
        e.presentation.isEnabled = !isTaskRunning
      }
    }

    return arrayOf(upgradeAllButton, refreshButton)
  }

  override fun canUninstallPackage(aPackage: InstalledPackage?): Boolean {
    return manager.interpreter != null && !isTaskRunning
  }

  override fun createManagePackagesDialog(): ManagePackagesDialog {
    return RManagePackagesDialog(myProject, rPackageManagementService, listener, this)
  }

  override fun canInstallPackage(aPackage: InstalledPackage): Boolean {
    return manager.interpreter != null && !isTaskRunning
  }

  override fun canUpgradePackage(aPackage: InstalledPackage?): Boolean {
    return manager.interpreter != null && !isTaskRunning
  }

  override fun updatePackages(packageManagementService: PackageManagementService?) {
    if (!this::rPackageManagementService.isInitialized) {
      rPackageManagementService = packageManagementService as RPackageManagementService
      immediatelyUpdatePackages(packageManagementService)
    } else {
      scheduleRefresh()
    }
  }

  private fun immediatelyUpdatePackages(packageManagementService: PackageManagementService?) {
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
    private val UPGRADE_ALL_ICON = RIcons.Packages.UPGRADE_ALL
    private val REFRESH_TEXT = RBundle.message("packages.panel.refresh.text")
    private val REFRESH_ICON = AllIcons.Actions.Refresh
    private val REFRESH_TASK_IDENTITY = RBundle.message("packages.panel.refresh.task.identity")
    private val REFRESH_TASK_NAME = RBundle.message("packages.panel.refresh.task.name")
  }
}
