/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.configuration

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup
import icons.RIcons
import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.concurrency.isPending
import org.jetbrains.concurrency.isRejected
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
import org.jetbrains.r.execution.ExecuteExpressionUtils
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterLocation
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.settings.RInterpreterSettings
import org.jetbrains.r.settings.RInterpreterSettingsProvider
import org.jetbrains.r.settings.RSettings

private const val rInterpreterWidgetId: String = "rInterpreterWidget"

class RInterpreterBarWidgetFactory : StatusBarWidgetFactory {

  override fun getId(): String = rInterpreterWidgetId

  override fun getDisplayName(): String = RBundle.message("interpreter.status.bar.display.name")

  override fun isAvailable(project: Project): Boolean = true

  override fun createWidget(project: Project): StatusBarWidget = RInterpreterStatusBarWidget(project)

  override fun disposeWidget(widget: StatusBarWidget) = Disposer.dispose(widget)

  override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}

private class RInterpreterStatusBarWidget(project: Project) : EditorBasedStatusBarPopup(project, false) {

  private val settings = RSettings.getInstance(project)
  @Volatile
  private var fetchInterpreterPromise: CancellablePromise<RInterpreterInfo?>? = null

  @Synchronized
  private fun fetchInterpreterAndUpdateWidgetState() {
    fetchInterpreterPromise?.cancel()
    fetchInterpreterPromise = runAsync {
      val projectInterpreterLocation = settings.interpreterLocation
      RInterpreterUtil.suggestAllInterpreters(true).firstOrNull { it.interpreterLocation == projectInterpreterLocation }
    }.onProcessed { update() } as CancellablePromise
  }

  override fun getWidgetState(file: VirtualFile?): WidgetState {
    val projectInterpreterInfo: RInterpreterInfo?
    synchronized(this) {
      val fetchPromise = fetchInterpreterPromise
      if (fetchPromise == null || fetchPromise.isPending) {
        fetchInterpreterAndUpdateWidgetState()
        return WidgetState.NO_CHANGE
      }
      projectInterpreterInfo = when {
        fetchPromise.isSucceeded -> {
          fetchInterpreterPromise = null
          fetchPromise.blockingGet(1)
        }
        fetchPromise.isRejected -> {
          fetchInterpreterPromise = null
          null
        }
        else -> return WidgetState.NO_CHANGE
      }
    }
    return if (projectInterpreterInfo == null) {
      WidgetState("", RBundle.message("interpreter.status.bar.no.interpreter"), true)
    }
    else {
      val descriptionRepresentation = projectInterpreterInfo.stringRepresentation
      val additionalSuffix = projectInterpreterInfo.interpreterLocation.additionalShortRepresentationSuffix().let {
        if (it.isNotBlank()) "($it)" else ""
      }
      val versionWithSuffix = "${projectInterpreterInfo.version} $additionalSuffix"
      WidgetState(RBundle.message("interpreter.status.bar.current.interpreter.tooltip", descriptionRepresentation),
                  RBundle.message("interpreter.status.bar.current.interpreter.name", versionWithSuffix), true)
    }
  }

  override fun isEnabledForFile(file: VirtualFile?): Boolean = true

  override fun registerCustomListeners() {
    RSettings.getInstance(project).addInterpreterLocationListener(object : RSettings.RInterpreterLocationListener {
      override fun projectInterpreterLocationChanged(actualInterpreterLocation: RInterpreterLocation?) = update()
    }, this)
  }

  override fun createPopup(context: DataContext): ListPopup? = RInterpreterPopupFactory(project).createPopup(context)

  override fun ID(): String = rInterpreterWidgetId

  override fun createInstance(project: Project): StatusBarWidget = RInterpreterStatusBarWidget(project)
}

class RInterpreterPopupFactory(private val project: Project) {

  private val settings = RSettings.getInstance(project)

  fun createPopup(context: DataContext): ListPopup? {
    val group = DefaultActionGroup()
    val allInterpreters = ExecuteExpressionUtils.getSynchronously(RBundle.message("project.settings.interpreters.loading")) {
      RInterpreterUtil.suggestAllInterpreters(true)
    }
    val groupedInterpreters = allInterpreters.groupBy { it.interpreterLocation.javaClass }
    groupedInterpreters.forEach { (_, interpreterGroup) ->
      group.addAll(interpreterGroup.map { SwitchToRInterpreterAction(it) })
      group.addSeparator()
    }

    group.add(RInterpreterSettingsAction())
    RInterpreterSettingsProvider.getProviders().forEach {
      group.add(AddRInterpreterAction(it, allInterpreters))
    }

    val currentInterpreterLocation = settings.interpreterLocation
    return JBPopupFactory.getInstance().createActionGroupPopup(
      RBundle.message("interpreter.status.bar.popup.title"),
      group,
      context,
      JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
      false,
      null,
      -1,
      Condition { it is SwitchToRInterpreterAction && it.interpreterInfo.interpreterLocation == currentInterpreterLocation },
      null
    ).apply { setHandleAutoSelectionBeforeShow(true) }
  }

  private fun switchToInterpreter(interpreterInfo: RInterpreterInfo) {
    val previousLocation = settings.interpreterLocation
    if (interpreterInfo.interpreterLocation != previousLocation) {
      settings.interpreterLocation = interpreterInfo.interpreterLocation
      if (!project.isDefault) {
        RInterpreterManager.restartInterpreter(project)
      }
    }
  }

  private inner class SwitchToRInterpreterAction(val interpreterInfo: RInterpreterInfo) : DumbAwareAction() {
    init {
      val presentation = templatePresentation
      val representation = interpreterInfo.stringRepresentation
      presentation.setText(representation, false)
      presentation.description = RBundle.message("interpreter.status.bar.switch.to.action.description", representation)
      presentation.icon = RIcons.R_logo_16
    }

    override fun actionPerformed(e: AnActionEvent) = switchToInterpreter(interpreterInfo)
  }

  private inner class RInterpreterSettingsAction : DumbAwareAction(RBundle.message("interpreter.status.bar.settings.action.name")) {
    override fun actionPerformed(e: AnActionEvent) {
      ShowSettingsUtil.getInstance().showSettingsDialog(project, RSettingsProjectConfigurable::class.java)
    }
  }

  private inner class AddRInterpreterAction(private val provider: RInterpreterSettingsProvider,
                                            private val allInterpreters: List<RInterpreterInfo>)
    : DumbAwareAction(provider.getAddInterpreterActionName()) {
    override fun actionPerformed(e: AnActionEvent) {
      provider.showAddInterpreterDialog(allInterpreters) {
        RInterpreterSettings.setEnabledInterpreters(allInterpreters + it)
        switchToInterpreter(it)
      }
    }
  }
}

private val RInterpreterInfo.stringRepresentation: String
  get() = "${interpreterName} (${version}) ${interpreterLocation}"