package org.jetbrains.r.misc

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.editor.WelcomeGuideManager

class RWelcomeGuideTest : RUsefulTestCase() {

  fun testShowWelcomeGuide() {
    PropertiesComponent.getInstance().unsetValue(WelcomeGuideManager.KEY)
    val project = myFixture.project
    val service = project.service<WelcomeGuideManager>()
    val fileEditorManager = FileEditorManager.getInstance(project)
    assertTrue(fileEditorManager.openFiles.isEmpty())
    service.showWelcomeGuide()
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    assertTrue(fileEditorManager.openFiles.isNotEmpty())
    fileEditorManager.closeFile(fileEditorManager.openFiles[0])
    service.showWelcomeGuide()
    assertTrue(fileEditorManager.openFiles.isEmpty())
  }
}