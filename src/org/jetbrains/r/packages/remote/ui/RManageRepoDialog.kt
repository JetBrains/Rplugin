// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CheckBoxList
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBCheckBox
import org.jetbrains.r.RBundle
import org.jetbrains.r.packages.remote.*
import org.jetbrains.r.packages.remote.RepoUtils.CRAN_URL_PLACEHOLDER
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

class RManageRepoDialog(
  private val project: Project,
  private val controller: RPackageManagementService,
  private val onModified: (Boolean) -> Unit
) : DialogWrapper(project, false) {

  private val mainPanel: JPanel
  private val list = RepositoryCheckBoxList()
  private val refreshCheckBox = JBCheckBox(REFRESH_CHECKBOX_TEXT, true)
  private var currentCranMirror: Int = 0

  private val currentSelection: RRepository?
    get() = list.getItemAt(list.selectedIndex)

  init {
    title = TITLE
    mainPanel = JPanel()
    mainPanel.layout = BoxLayout(mainPanel, BoxLayout.PAGE_AXIS)
    list.setCheckBoxListListener { _, _ ->
      checkActiveExists()
    }
    val repositoryList = createRepositoriesList()
    mainPanel.add(repositoryList)
    mainPanel.add(Box.createRigidArea(Dimension(0, 10)))
    mainPanel.add(refreshCheckBox)
    reloadList()
    init()
  }

  private fun checkActiveExists() {
    fun CheckBoxList<RRepository>.getActiveRepositories(): List<RRepository> {
      return boolMap { repository, isSelected -> if (isSelected) repository else null }
    }

    if (list.getActiveRepositories().isEmpty()) {
      val matches = list.boolMap { repository, _ ->
        if (repository.url == CRAN_URL_PLACEHOLDER) repository else null
      }
      matches.firstOrNull()?.let { repository ->
        list.setItemSelected(repository, true)
      }
    }
  }

  private fun <R>CheckBoxList<RRepository>.boolMap(mapping: (RRepository, Boolean) -> R?): List<R> {
    return mutableListOf<R>().also {
      for (i in 0 until itemsCount) {
        getItemAt(i)?.let { repository ->
          val isSelected = isItemSelected(i)
          mapping(repository, isSelected)?.let { result ->
            it.add(result)
          }
        }
      }
    }
  }

  private fun CheckBoxList<RRepository>.getRepositorySelections(): List<Pair<RRepository, Boolean>> {
    return boolMap { repository, isSelected -> Pair(repository, isSelected) }
  }

  private fun reloadList() {
    list.clear()
    currentCranMirror = controller.cranMirror
    val allRepositories = controller.defaultRepositories + controller.userRepositories
    val enabledUrls = controller.enabledRepositoryUrls
    for (repository in allRepositories) {
      list.addItem(repository, repository.url, enabledUrls.contains(repository.url))
      list.model.getElementAt(list.itemsCount - 1).apply {
        isEnabled = repository.isOptional
        toolTipText = if (!repository.isOptional) DISABLED_CHECKBOX_HINT else null
      }
    }
    checkActiveExists()
  }

  private fun createRepositoriesList(): JPanel {
    return ToolbarDecorator.createDecorator(list)
      .disableUpDownActions()
      .setAddAction { addAction() }
      .setEditAction { editAction() }
      .setEditActionUpdater { currentSelection !is RDefaultRepository || currentSelection?.url == CRAN_URL_PLACEHOLDER }
      .setRemoveAction { removeAction() }
      .setRemoveActionUpdater { currentSelection !is RDefaultRepository }
      .createPanel()
  }

  private fun addAction() {
    val url = Messages.showInputDialog(ADD_REPOSITORY_MESSAGE, ADD_REPOSITORY_TITLE, null)
    if (url != null && !url.isBlank()) {
      list.addItem(RUserRepository(url), url, true)
    }
  }

  private fun editAction() {
    val oldValue = currentSelection
    if (oldValue != null && oldValue.url == CRAN_URL_PLACEHOLDER) {
      editCranMirrorAction()
    } else {
      editUserRepositoryAction()
    }
  }

  private fun editCranMirrorAction() {
    RChooseMirrorDialog(controller.mirrors, currentCranMirror) { choice ->
      currentCranMirror = choice
    }.show()
  }

  private fun editUserRepositoryAction() {
    val oldValue = currentSelection
    val url = Messages.showInputDialog(EDIT_REPOSITORY_MESSAGE, EDIT_REPOSITORY_TITLE, null, oldValue!!.url, object : InputValidator {
      override fun checkInput(inputString: String): Boolean {
        return inputString.isNotBlank() && inputString != CRAN_URL_PLACEHOLDER
      }

      override fun canClose(inputString: String): Boolean {
        return true
      }
    })
    if (url != null && url.isNotBlank() && oldValue.url != url) {
      if (oldValue is RUserRepository) {  // Double check
        list.updateItem(oldValue, RUserRepository(url), url)
      }
    }
  }

  private fun removeAction() {
    fun removeUserRepository(selected: RUserRepository) {
      val repositorySelections = list.getRepositorySelections()
      list.clear()
      for ((repository, isSelected) in repositorySelections) {
        if (repository != selected) {
          list.addItem(repository, repository.url, isSelected)
        }
      }
      checkActiveExists()
    }

    val index = list.selectedIndex
    val selected = list.getItemAt(index)
    if (selected is RUserRepository) {  // Double check
      removeUserRepository(selected)
    }
  }

  override fun doOKAction() {
    processDoNotAskOnOk(0)
    if (okAction.isEnabled) {
      controller.cranMirror = currentCranMirror
      controller.setRepositories(list.getRepositorySelections())
      RepoUtils.resetPackageDetails(project)  // List of selected repositories may change => invalidate cache
      onModified(refreshCheckBox.isSelected)
      close(0)
    }
  }

  override fun createCenterPanel(): JComponent? {
    return mainPanel
  }

  // Note: you may expect that checkbox will be rendered as disabled
  // after you find it in the list model and manually disable via `isEnabled = false`.
  // But wait... For some strange reasons actually it won't.
  // That's why the `CheckBoxList` subclass below was introduced
  private class RepositoryCheckBoxList : CheckBoxList<RRepository>() {
    override fun isEnabled(index: Int): Boolean {
      return model.getElementAt(index).isEnabled
    }
  }

  companion object {
    private val TITLE = RBundle.message("repo.dialog.title")
    private val ADD_REPOSITORY_TITLE = RBundle.message("repo.dialog.add.repository.title")
    private val ADD_REPOSITORY_MESSAGE = RBundle.message("repo.dialog.add.repository.message")
    private val EDIT_REPOSITORY_TITLE = RBundle.message("repo.dialog.edit.repository.title")
    private val EDIT_REPOSITORY_MESSAGE = RBundle.message("repo.dialog.edit.repository.message")
    private val REFRESH_CHECKBOX_TEXT = RBundle.message("repo.dialog.refresh.checkbox.text")
    private val GETTING_AVAILABLE_REPOSITORIES = RBundle.message("repo.dialog.getting.available.repositories")
    private val GETTING_AVAILABLE_MIRRORS = RBundle.message("repo.dialog.getting.available.mirrors")
    private val DISABLED_CHECKBOX_HINT = RBundle.message("repo.dialog.disabled.checkbox.hint")
  }
}
