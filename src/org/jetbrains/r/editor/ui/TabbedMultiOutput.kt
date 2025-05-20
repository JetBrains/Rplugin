/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Disposer
import com.intellij.ui.Gray
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.TabsListener
import com.intellij.ui.tabs.impl.JBTabsImpl
import org.jetbrains.r.visualization.inlays.InlayOutputData
import org.jetbrains.r.visualization.inlays.MouseWheelUtils
import org.jetbrains.r.visualization.inlays.components.InlayProgressStatus
import org.jetbrains.r.visualization.inlays.components.NotebookInlayMultiOutput
import org.jetbrains.r.visualization.inlays.components.NotebookInlayState
import org.jetbrains.r.visualization.inlays.components.ToolBarProvider
import org.jetbrains.r.visualization.ui.ToolbarUtil
import java.awt.BorderLayout
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JComponent
import javax.swing.JPanel

/** A multi-output inlay that puts outputs from different sources to separate tabbed pane tabs. */
class TabbedMultiOutput(private val editor: Editor, parent: Disposable) : NotebookInlayMultiOutput() {
  /** Page control for result viewing. */
  private val tabs: JBTabsImpl

  var onChange: (() -> Unit)? = null

  private val disposable = Disposer.newDisposable()

  private var maxHeight: Int = -1

  private val project = editor.project!!

  private val tabsOutput: MutableSet<NotebookInlayOutput> = mutableSetOf()

  @Volatile
  private var isInViewport: Boolean = false

  init {
    Disposer.register(parent, disposable)
    tabs = JBTabsImpl(project, disposable)
    tabs.addListener(object : TabsListener {
      override fun selectionChanged(oldSelection: TabInfo?, newSelection: TabInfo?) {
        oldSelection?.onViewportChange(false)  // Definitely false
        newSelection?.onViewportChange(isInViewport)  // Might be true
        onChange?.invoke()
      }
    })

    tabs.component.isOpaque = false
    tabs.component.background = Gray.TRANSPARENT

    MouseWheelUtils.wrapMouseWheelListeners(tabs.component, parent)
    add(tabs.component, BorderLayout.CENTER)

    // To make it possible to use JLayeredPane as a parent of NotebookInlayState.
    addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        tabs.component.bounds = Rectangle(0, 0, e.component.bounds.width, e.component.bounds.height)
      }
    })
  }

  fun setCurrentPage(currentPage: String) {
    val tabToSelect = tabs.tabs.find { it.text == currentPage }
    if(tabToSelect != null) {
      tabs.select(tabToSelect, false)
    }
  }

  override fun onOutputs(inlayOutputs: List<InlayOutputData>) {
    tabs.removeAllTabs()
    tabsOutput.clear()
    for (inlayOutput in inlayOutputs) {
      NotebookInlayOutput(editor, disposable).apply {
        setupOnHeightCalculated()
        addData(inlayOutput.type, inlayOutput.data)
        tabsOutput.add(this)
        addTab(inlayOutput)
      }
    }
  }

  override fun updateProgressStatus(progressStatus: InlayProgressStatus) {
    tabsOutput.forEach { output ->
      output.updateProgressStatus(progressStatus)
    }
  }

  override fun clear() {
  }

  override fun getCollapsedDescription(): String {
    return "foooo"
  }

  override fun onViewportChange(isInViewport: Boolean) {
    this.isInViewport = isInViewport
    tabs.selectedInfo?.onViewportChange(isInViewport)
  }

  private fun TabInfo.onViewportChange(isInViewport: Boolean) {
    (component as NotebookInlayState?)?.onViewportChange(isInViewport)
  }

  private fun NotebookInlayState.setupOnHeightCalculated() {
    onHeightCalculated = {
      tabs.findInfo(this)?.let { tab ->
        updateMaxHeight(it + tabs.getTabLabel(tab)!!.preferredSize.height)
      }
    }
  }

  private fun NotebookInlayState.addTab(inlayOutput: InlayOutputData) {
    addTab(TabInfo(this).apply {
      inlayOutput.preview?.let {
        setIcon(it)
        setText("")
      }
    }).apply {
      if (tabs.selectedInfo == null) {
        tabs.select(this, false)
      }
    }
  }

  private fun addTab(tabInfo: TabInfo, select: Boolean = false): TabInfo {
    // We need to set empty DefaultActionGroup to move sideComponent to the right.
    tabInfo.setActions(DefaultActionGroup(), ActionPlaces.UNKNOWN)
    tabInfo.setSideComponent(createTabToolbar(tabInfo))
    tabs.addTab(tabInfo)
    if (select) {
      tabs.select(tabInfo, false)
    }
    return tabInfo
  }

  private fun createTabToolbar(tabInfo: TabInfo): JComponent {
    val actionGroups = createTabActionGroups(tabInfo)
    val toolbar = ToolbarUtil.createActionToolbar(javaClass.simpleName, actionGroups)
    toolbar.targetComponent = tabInfo.component
    val toolbarComponent = toolbar.component
    if (toolbarComponent is ActionToolbarImpl) {
      toolbarComponent.setForceMinimumSize(true)
    }
    return JPanel().apply {  // Align toolbar to top
      add(toolbarComponent)
    }
  }

  private fun createTabActionGroups(tabInfo: TabInfo): List<List<AnAction>> {
    return mutableListOf<List<AnAction>>().also { groups ->
      (tabInfo.component as? ToolBarProvider)?.let { provider ->
        groups.add(provider.createActions())
      }
    }
  }

  private fun updateMaxHeight(height: Int) {
    if (maxHeight < height) {
      maxHeight = height
      onHeightCalculated?.invoke(maxHeight)
    }
  }
}
