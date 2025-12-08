// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote.ui

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages.showInfoMessage
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.execution.ExecuteExpressionUtils.getSynchronously
import com.intellij.r.psi.icons.RIcons
import com.intellij.r.psi.interpreter.RInterpreterManager
import com.intellij.r.psi.interpreter.RLibraryWatcher
import com.intellij.r.psi.packages.RInstalledPackage
import com.intellij.r.psi.packages.RPackageVersion
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import com.intellij.webcore.packaging.ManagePackagesDialog
import com.intellij.webcore.packaging.PackageManagementService
import com.intellij.webcore.packaging.PackagesNotificationPanel
import com.intellij.webcore.packaging.RepoPackage
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.packages.remote.RPackageManagementService
import com.intellij.r.psi.visualization.ui.ToolbarUtil

class RInstalledPackagesPanel(private val project: Project, area: PackagesNotificationPanel) :
  RInstalledPackagesPanelBase(project, area), RPackageServiceListener {

  private val queue = MergingUpdateQueue(RBundle.message("packages.panel.refresh.task.name"), REFRESH_TIME_SPAN, true, null, project)

  private val listener = object : PackageManagementService.Listener {
    override fun operationStarted(packageName: String?) {
    }

    override fun operationFinished(packageName: String?, errorDescription: PackageManagementService.ErrorDescription?) {
      myNotificationArea.showResult(packageName, errorDescription)
      myPackagesTable.clearSelection()
      doUpdatePackages(myPackageManagementService)
    }
  }

  private val isReady: Boolean
    get() = rPackageManagementService != null && RInterpreterManager.getInterpreterOrNull(project) != null && !isTaskRunning

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

  override fun getExtraActions(): Array<AnAction> {
    return arrayOf(makeUpgradeAllButton(), makeRefreshButton())
  }

  private fun makeUpgradeAllButton(): AnAction {
    ActionManager.getInstance().getAction(UPGRADE_ALL_ACTION_ID).templatePresentation.icon = RIcons.Packages.UpgradeAll
    return ToolbarUtil.createAnActionButton(UPGRADE_ALL_ACTION_ID, this::canUpgradeAllPackages, this::upgradeAllPackages)
  }

  private fun upgradeAllPackages() {
    rPackageManagementService?.let { service ->
      getSynchronously(RBundle.message("packages.panel.getting.available.packages.title")) {
        service.allPackages
      }
      runAsync {
        val outdated = findOutdatedPackages(service)
        invokeLater {
          if (outdated.isNotEmpty()) {
            RUpdateAllConfirmDialog(outdated) {
              val packages = outdated.map { RepoPackage(it.installedPackage.name, null, null) }
              val multiListener = RPackageManagementService.convertToInstallMultiListener(listener)
              service.installPackages(packages, true, multiListener)
            }.show()
          } else {
            showInfoMessage(RBundle.message("packages.panel.nothing.to.upgrade.message"), RBundle.message("packages.panel.nothing.to.upgrade.title"))
          }
        }
      }
    }
  }

  private fun canUpgradeAllPackages() = isReady

  private fun findOutdatedPackages(service: RPackageManagementService): List<RPackageUpdateInfo> {
    return service.installedPackagesList.mapNotNull { installed ->
      service.fetchLatestVersion(installed.version)?.let { latestVersion ->
        if (RPackageVersion.isOlder(installed.version, latestVersion)) RPackageUpdateInfo(installed, latestVersion) else null
      }
    }
  }

  private fun makeRefreshButton(): AnAction {
    return ToolbarUtil.createAnActionButton(REFRESH_ACTION_ID, { !isTaskRunning }) {
      immediatelyUpdatePackages(myPackageManagementService)
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
    queue.queue(object : Update(RBundle.message("packages.panel.refresh.task.identity")) {
      override fun run() {
        immediatelyUpdatePackages(myPackageManagementService)
      }
    })
  }

  companion object {
    private const val UPGRADE_ALL_ACTION_ID = "org.jetbrains.r.packages.remote.ui.RUpgradeAllAction"
    private const val REFRESH_ACTION_ID = "org.jetbrains.r.packages.remote.ui.RRefreshAction"
    private const val REFRESH_TIME_SPAN = 500
  }
}
