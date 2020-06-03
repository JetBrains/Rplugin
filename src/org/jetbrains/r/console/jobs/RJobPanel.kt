/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.UIUtilities
import com.intellij.util.ui.components.BorderLayoutPanel
import net.miginfocom.swing.MigLayout
import org.apache.commons.lang.time.DurationFormatUtils
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
import org.jetbrains.r.RESTART_JOB
import org.jetbrains.r.R_LOGO_16
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.*

private interface SplitterApi {
  fun setLeftComponent(component: JComponent)
  fun setRightComponent(component: JComponent)
  fun restoreDefaults()
}

class RJobPanel(private val project: Project) : BorderLayoutPanel() {
  private val runJob = ActionManager.getInstance().getAction("org.jetbrains.r.console.jobs.RunRJobAction")
  private val jbSplitter = OnePixelSplitter(false, 0.4f)
  private val splitterApi = object : SplitterApi {
    override fun setLeftComponent(component: JComponent) {
      jbSplitter.firstComponent = component
      jbSplitter.repaint()
      jbSplitter.invalidate()
    }

    override fun setRightComponent(component: JComponent) {
      jbSplitter.secondComponent = component
      jbSplitter.repaint()
      jbSplitter.invalidate()
    }

    override fun restoreDefaults() {
      jbSplitter.firstComponent = emptyLeftComponent
      jbSplitter.secondComponent = emptyRightComponent
      jbSplitter.repaint()
      jbSplitter.invalidate()
    }
  }
  private val jobList = JobList(splitterApi, project)
  private val emptyLeftComponent = object : BorderLayoutPanel() {
    init {
      background = backgroundColor()
      add(LinkLabel.create(RBundle.message("jobs.panel.start.new.job.label.text") ) {
        if (RJobRunner.getInstance(project).canRun()) {
          RunRJobAction.showDialog(project)
        }
      }.apply {
        horizontalAlignment = SwingConstants.CENTER
      }, BorderLayout.CENTER)
    }

    override fun updateUI() {
      super.updateUI()
      background = backgroundColor()
    }
  }

  private val emptyRightComponent = JPanel()

  init {
    splitterApi.restoreDefaults()
    addToCenter(jbSplitter)
    addToolbar()
  }

  fun addJobDescriptor(jobDescriptor: RJobDescriptor) {
    jobList.addJobDescriptor(jobDescriptor)
  }

  private fun addToolbar() {
    val actionGroup = DefaultActionGroup(AddJob(), RemoveCompletedJobs(), RerunJob())
    val toolbar = ActionManager.getInstance().createActionToolbar("", actionGroup, false)
    toolbar.setTargetComponent(this)
    addToLeft(toolbar.component)
  }

  private inner class AddJob : DumbAwareAction() {
    init {
      copyFrom(runJob)
      templatePresentation.icon =  AllIcons.General.Add
    }

    override fun actionPerformed(e: AnActionEvent) {
      runJob.actionPerformed(e)
    }

    override fun update(e: AnActionEvent) {
      runJob.update(e)
    }
  }

  override fun updateUI() {
    super.updateUI()
    background = backgroundColor()
  }

  private inner class RemoveCompletedJobs : DumbAwareAction(RBundle.message("jobs.panel.action.remove.completed.jobs.text"),
                                                            RBundle.message("jobs.panel.action.remove.completed.jobs.description"),
                                                            AllIcons.Actions.GC) {
    override fun actionPerformed(e: AnActionEvent) {
      jobList.jobEntities.filter { it.jobDescriptor.processTerminated }.forEach {
        jobList.removeJobEntity(it)
      }
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = jobList.jobEntities.any { it.jobDescriptor.processTerminated }
    }
  }

  private inner class RerunJob : DumbAwareAction(RBundle.message("jobs.panel.action.rerun.job.text"),
                                                 RBundle.message("jobs.panel.action.rerun.job.description"),
                                                 RESTART_JOB) {
    override fun actionPerformed(e: AnActionEvent) {
      jobList.currentlySelected?.let {
        jobList.removeJobEntity(it)
        runAsync {
          it.jobDescriptor.rerun()
        }
      }
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = jobList.currentlySelected?.jobDescriptor?.processTerminated == true
    }
  }
}

private class JobList(private val splitter: SplitterApi, private val project: Project) {
  private val panel =  object : JPanel(GridBagLayout()) {
    override fun updateUI() {
      super.updateUI()
      background = backgroundColor()
    }
  }
  private val emptyLabel = JLabel("")
  private val scrollPane = JBScrollPane()

  val jobEntities = ArrayList<JobEntity>()
  var currentlySelected : JobEntity? = null

  init {
    panel.background = backgroundColor()
    scrollPane.setViewportView(panel)
    scrollPane.border = null
    panel.border = null
    installMouseListeners(panel)
  }

  fun addJobDescriptor(jobDescriptor: RJobDescriptor) {
    if (jobEntityCount() == 0) {
      splitter.setLeftComponent(scrollPane)
    }
    val jobEntity = JobEntity(jobDescriptor, this)
    jobEntities.add(jobEntity)
    panel.remove(emptyLabel)
    val constraints = GridBag().weightx(1.0).anchor(GridBagConstraints.NORTH).fillCellHorizontally().coverLine()
      .insets(0, 0, 0, 0)
    panel.add(jobEntity, constraints)
    changeSelection(jobEntity)
    panel.add(emptyLabel, GridBag().weighty(1.0))
  }

  private fun installMouseListeners(panel: JPanel) {
    panel.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(event: MouseEvent) {
        val componentAtMouse = panel.getComponentAt(event.point)
        if (componentAtMouse is JobEntity) {
          changeSelection(componentAtMouse)
        }
      }
    })
    object : DoubleClickListener() {
      override fun onDoubleClick(event: MouseEvent): Boolean {
        val componentAtMouse = panel.getComponentAt(event.point)
        if (componentAtMouse is JobEntity) {
          if (openFileInEditor(componentAtMouse)) return true
          return true
        }
        return false
      }
    }.installOn(panel)
  }

  fun openFileInEditor(componentAtMouse: JobEntity): Boolean {
    val scriptFile = componentAtMouse.jobDescriptor.scriptFile
    val findFileByIoFile = VfsUtil.findFileByIoFile(scriptFile, true) ?: return true
    FileEditorManager.getInstance(project).openFile(findFileByIoFile, true)
    return false
  }

  fun changeSelection(jobEntity: JobEntity) {
    currentlySelected?.let {
      it.isSelected = false
      UIUtil.setBackgroundRecursively(it, backgroundColor())
    }
    currentlySelected = jobEntity
    jobEntity.isSelected = true
    UIUtil.setBackgroundRecursively(jobEntity, selectionColor())
    splitter.setRightComponent(jobEntity.jobDescriptor.outputComponent)
  }

  fun removeJobEntity(jobEntity: JobEntity) {
    panel.remove(jobEntity)
    val count = jobEntityCount()
    if (jobEntity.isSelected && count > 0) {
      changeSelection(panel.components[0] as JobEntity)
    }
    if (count == 0) {
      currentlySelected = null
      splitter.restoreDefaults()
    }
    if (!jobEntity.jobDescriptor.processTerminated) {
      jobEntity.jobDescriptor.destroyProcess()
    }
  }

  private fun jobEntityCount() = panel.components.count { it is JobEntity }
}

private class JobEntity(val jobDescriptor: RJobDescriptor,
                        private val jobList: JobList)
  : JPanel(MigLayout("insets 0 0 0 0", "[grow][][]", "[]")) {

  private var deleted = false
  private val rLogo = JBLabel(R_LOGO_16)
  private val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
    add(rLogo)
  }
  private val rightComponent: JComponent

  private var paintUnbounded: Boolean = false
  private val filename = jobDescriptor.scriptFile.name
  private val directoryName = FileUtil.getLocationRelativeToUserHome(
    LocalFileSystem.getInstance().extractPresentableUrl(jobDescriptor.scriptFile.parent))


  private val progressBar = JProgressBar().apply {
    minimumSize = Dimension(PROGRESS_BAR_WIDTH, minimumSize.height)
    maximumSize = Dimension(PROGRESS_BAR_WIDTH, maximumSize.height)
  }

  internal var isSelected = false

  init {
    addToLeft(leftPanel)
    installMouseListenerOnLeftPanel()
    rightComponent = createRightComponents()
    addToRight(rightComponent)

    background = backgroundColor()
    jobDescriptor.onProgressChanged { current, total ->
      invokeLater {
        if (deleted) return@invokeLater
        progressBar.value = current
        progressBar.maximum = total
        revalidate()
        repaint()
      }
    }
  }

  private fun installMouseListenerOnLeftPanel() {
    leftPanel.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent?) {
        jobList.changeSelection(this@JobEntity)
      }

      override fun mouseEntered(e: MouseEvent) {
        paintUnbounded = true
        revalidate()
        repaint()
      }

      override fun mouseExited(e: MouseEvent?) {
        paintUnbounded = false
        revalidate()
        repaint()
      }
    })

    object : DoubleClickListener() {
      override fun onDoubleClick(event: MouseEvent): Boolean {
        jobList.openFileInEditor(this@JobEntity)
        return true
      }

    }.installOn(leftPanel)
  }

  override fun updateUI() {
    super.updateUI()
    UIUtil.setBackgroundRecursively(this, if (isSelected) selectionColor() else backgroundColor())
  }

  private fun addToRight(centerComponent: JComponent) {
    add(centerComponent, "width ${PROGRESS_BAR_WIDTH}:${PROGRESS_BAR_WIDTH}:${PROGRESS_BAR_WIDTH}, right")
  }

  private fun addToLeft(jcomponent: JComponent) {
    add(jcomponent, "growx, left")
  }

  private fun updateCenterComponentsAfterTermination() {
    invokeLater {
      remove(rightComponent)
      addToRight(createTerminationStatusBar())
      revalidate()
      repaint()
    }
  }

  private fun createTerminationStatusBar(): JPanel {
    val panel = BorderLayoutPanel()
    panel.background = background
    val duration = JBLabel(
      if (jobDescriptor.processFailed) AllIcons.RunConfigurations.ToolbarError else AllIcons.RunConfigurations.ToolbarPassed)
    duration.text = DurationFormatUtils.formatDuration(jobDescriptor.duration, "mm:ss", true)
    duration.foreground = infoColor()
    panel.addToLeft(duration)
    val startDateTime = JBLabel(DateFormatUtil.formatDateTime(jobDescriptor.startedAt))
    startDateTime.foreground = infoColor()
    panel.addToRight(startDateTime)
    return panel
  }

  override fun paint(g: Graphics) {
    super.paint(g)

    val x = leftPanel.x
    val y = leftPanel.y
    val width = leftPanel.width
    val height = leftPanel.height

    val graphics = g.create() as Graphics2D
    val unbounded = paintUnbounded
    try {
      val logoWidth = rLogo.width + rLogo.insets.left + rLogo.insets.right
      val filenameWidth = UIUtilities.stringWidth(this, getFontMetrics(font), filename)
      val directoryNameWidth = UIUtilities.stringWidth(this, getFontMetrics(font), directoryName)

      val firstTextWidth = if (unbounded) filenameWidth else width - rLogo.width - LOGO_OFFSET
      val secondTextWidth = if (unbounded) directoryNameWidth else width - (logoWidth + filenameWidth + LOGO_OFFSET * 2)

      fun fillRectangle() {
        val oldColor = graphics.color
        val oldBackground = graphics.background
        graphics.color = background
        graphics.fillRect(x + logoWidth + LOGO_OFFSET, y, filenameWidth + directoryNameWidth + LOGO_OFFSET, height )
        graphics.color = oldColor
        graphics.background = oldBackground
      }

      if (unbounded) {
        fillRectangle()
      }

      val firstTextRec = Rectangle(x + logoWidth + LOGO_OFFSET, y, firstTextWidth, height)
      UIUtil.drawCenteredString(graphics, firstTextRec, filename, false, true)
      val oldColor = graphics.color
      graphics.color = infoColor()
      val secondTextRec = Rectangle(x + logoWidth + filenameWidth + LOGO_OFFSET * 2, y, secondTextWidth, height)
      UIUtil.drawCenteredString(graphics, secondTextRec, directoryName, false, true)
      graphics.color = oldColor

    } finally {
      graphics.dispose()
    }
  }

  private fun createRightComponents(): JComponent {
    val statusBarCreated = AtomicBoolean(false)
    jobDescriptor.onProcessTerminated {
      invokeLater {
        if (deleted || !statusBarCreated.get()) return@invokeLater
        updateCenterComponentsAfterTermination()
      }
    }
    if (jobDescriptor.processTerminated) {
      return createTerminationStatusBar()
    }
    statusBarCreated.set(true)
    val action = object : AnAction(RBundle.message("jobs.panel.action.terminate.text"),
                                   RBundle.message("jobs.panel.action.terminate.description"),
                                   AllIcons.Actions.CloseHovered) {
      override fun actionPerformed(e: AnActionEvent) {
        jobList.removeJobEntity(this@JobEntity)
        deleted = true
      }
    }
    val actionButton = object : ActionButton(action, action.templatePresentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE) {
      override fun setBackground(bg: Color?) {
        super.setBackground(null)
      }
    }
    actionButton.background = null
    val panel = BorderLayoutPanel()
    panel.addToCenter(progressBar)
    panel.addToRight(actionButton)
    return panel
  }
}

private fun backgroundColor() = UIUtil.getEditorPaneBackground()
private fun selectionColor() = EditorColorsUtil.getGlobalOrDefaultColorScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES).backgroundColor
private fun infoColor() = UIUtil.getInactiveTextColor()

private val PROGRESS_BAR_WIDTH = JBUIScale.scale(150)
private val LOGO_OFFSET = JBUIScale.scale(6)