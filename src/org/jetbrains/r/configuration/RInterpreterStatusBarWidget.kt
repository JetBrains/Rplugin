/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.configuration

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.RPluginCoroutineScope
import com.intellij.r.psi.configuration.RSettingsProjectConfigurable
import com.intellij.r.psi.icons.RIcons
import com.intellij.r.psi.interpreter.RInterpreterInfo
import com.intellij.r.psi.interpreter.RInterpreterLocation
import com.intellij.r.psi.interpreter.RInterpreterManager
import com.intellij.r.psi.interpreter.RInterpreterUtil
import com.intellij.r.psi.interpreter.RLocalInterpreterLocation
import com.intellij.r.psi.settings.RInterpreterSettings
import com.intellij.r.psi.settings.RInterpreterSettingsProvider
import com.intellij.r.psi.settings.RSettings
import com.intellij.util.messages.MessageBusConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.Nls
import org.jetbrains.r.console.RConsoleToolWindowFactory

private const val ID: String = "rInterpreterWidget"

internal class RInterpreterBarWidgetFactory : StatusBarWidgetFactory {
  override fun getId(): String = ID

  override fun getDisplayName(): String = RBundle.message("interpreter.status.bar.display.name")

  override fun isAvailable(project: Project): Boolean {
    return RConsoleToolWindowFactory.getRConsoleToolWindows(project)?.contentManager?.contents?.isNotEmpty() ?: false
  }

  override fun createWidget(project: Project, scope: CoroutineScope): StatusBarWidget = RInterpreterStatusBarWidget(project, scope)

  companion object {
    fun updateWidget(project: Project) {
      project.service<StatusBarWidgetsManager>().updateWidget(RInterpreterBarWidgetFactory::class.java)
    }
  }
}

private class RInterpreterStatusBarWidget(project: Project,
                                          scope: CoroutineScope) : EditorBasedStatusBarPopup(project = project,
                                                                                             isWriteableFileRequired = false,
                                                                                             scope = scope) {
  private val settings = RSettings.getInstance(project)

  @Volatile
  private var fetchInterpreterPromise: Deferred<RInterpreterInfo?>? = null
  @Volatile
  private var cachedValue: Pair<RInterpreterInfo?, Long> = null to 0L

  private val freshnessMillis: Long = 2000L

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun getWidgetState(file: VirtualFile?): WidgetState {
    val now = System.currentTimeMillis()
    val projectInterpreterInfo: RInterpreterInfo? = synchronized(this) {
      val (cached, lastFetchFinishedAtMillis) = cachedValue
      val fresh = lastFetchFinishedAtMillis != 0L && (now - lastFetchFinishedAtMillis) < freshnessMillis
      val promise = fetchInterpreterPromise

      when {
        fresh -> cached

        promise == null || promise.isCancelled || promise.isCompleted -> {
          fetchInterpreterPromise = RPluginCoroutineScope.getScope(project).async {
            val projectInterpreterLocation = settings.interpreterLocation
            val fetched = RInterpreterUtil.suggestAllInterpreters(true)
              .firstOrNull { it.interpreterLocation == projectInterpreterLocation }
            cachedValue = fetched to System.currentTimeMillis()
            fetched
          }
          RPluginCoroutineScope.getScope(project).async {
            fetchInterpreterPromise?.await()
            withContext(Dispatchers.EDT) { update() }
          }
          return WidgetState.NO_CHANGE
        }

        promise.isActive -> cached

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

  override fun registerCustomListeners(connection: MessageBusConnection) {
    RSettings.getInstance(project).addInterpreterLocationListener(object : RSettings.RInterpreterLocationListener {
      override fun projectInterpreterLocationChanged(actualInterpreterLocation: RInterpreterLocation?) = update()
    }, this)
  }

  override fun createPopup(context: DataContext): ListPopup = RInterpreterPopupFactory(project).createPopup(context)

  override fun ID(): String = ID

  override fun createInstance(project: Project): StatusBarWidget = RInterpreterStatusBarWidget(project, scope)
}

class RInterpreterPopupFactory(private val project: Project) {

  private val settings = RSettings.getInstance(project)

  fun createPopup(context: DataContext): ListPopup {
    val group = DefaultActionGroup()
    val allInterpreters = runWithModalProgressBlocking(project, RBundle.message("project.settings.interpreters.loading")) {
      RInterpreterUtil.suggestAllInterpreters(true)
    }
    val groupedInterpreters = allInterpreters.groupBy { it.interpreterLocation.javaClass }
    groupedInterpreters.forEach { (_, interpreterGroup) ->
        group.addSeparator(interpreterGroup.first().interpreterLocation.getWidgetSwitchInterpreterActionHeader())
        group.addAll(interpreterGroup.map { SwitchToRInterpreterAction(it) })
      }

    group.addSeparator(RBundle.message("interpreter.status.bar.add.new.header"))
    RInterpreterSettingsProvider.getProviders().forEach {
      group.add(AddRInterpreterAction(it, allInterpreters))
    }
    group.addSeparator()
    group.add(RInterpreterSettingsAction())

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
        RInterpreterManager.getInstance(project).restartInterpreter()
      }
    }
  }

  private inner class SwitchToRInterpreterAction(val interpreterInfo: RInterpreterInfo) : DumbAwareAction() {
    init {
      val presentation = templatePresentation
      val representation = interpreterInfo.stringRepresentation
      presentation.setText(representation, false)
      presentation.description = RBundle.message("interpreter.status.bar.switch.to.action.description", representation)
      presentation.icon = RIcons.R
    }

    override fun actionPerformed(e: AnActionEvent) = switchToInterpreter(interpreterInfo)
  }

  private inner class RInterpreterSettingsAction : DumbAwareAction(RBundle.message("interpreter.status.bar.settings.action.name"),
                                                                   RBundle.message("interpreter.status.bar.settings.action.description"),
                                                                   null) {
    override fun actionPerformed(e: AnActionEvent) {
      ShowSettingsUtil.getInstance().showSettingsDialog(project, RSettingsProjectConfigurable::class.java)
    }
  }

  private inner class AddRInterpreterAction(private val provider: RInterpreterSettingsProvider,
                                            private val allInterpreters: List<RInterpreterInfo>)
    : DumbAwareAction(provider.getAddInterpreterWidgetActionName(),
                      provider.getAddInterpreterWidgetActionDescription(),
                      provider.getAddInterpreterWidgetActionIcon()) {
    override fun actionPerformed(e: AnActionEvent) {
      provider.showAddInterpreterDialog(allInterpreters) {
        RInterpreterSettings.setEnabledInterpreters(allInterpreters + it)
        switchToInterpreter(it)
      }
    }
  }
}

private val RInterpreterInfo.stringRepresentation: String
  @Nls
  get() {
    return if (this.interpreterLocation is RLocalInterpreterLocation) RBundle.message(
      "interpreter.status.bar.local.interpreter.representation", version)
    else "${interpreterName} (${version})"
  }
