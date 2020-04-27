/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.settings

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PermanentInstallationID
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.util.io.HttpRequests
import org.jdom.JDOMException
import org.jetbrains.r.RFileType
import org.jetbrains.r.RPluginUtil
import java.net.URLEncoder
import java.util.concurrent.TimeUnit


private const val KEY = "r.last.update.timestamp"


class RUpdateComponent : EditorFactoryListener {
  override fun editorCreated(event: EditorFactoryEvent) {
    if (ApplicationManager.getApplication().isUnitTestMode) return
    val document = event.editor.document

    val file = FileDocumentManager.getInstance().getFile(document)
    if (file != null && (file.fileType is RFileType || file.extension?.toLowerCase() == "rmd")) {
      checkForUpdates()
    }
  }

  private fun checkForUpdates() {
    val propertiesComponent = PropertiesComponent.getInstance()
    val lastUpdate = propertiesComponent.getOrInitLong(KEY, 0)
    if (lastUpdate == 0L || System.currentTimeMillis() - lastUpdate > TimeUnit.DAYS.toMillis(1)) {
      ApplicationManager.getApplication().executeOnPooledThread {
        try {
          val buildNumber = ApplicationInfo.getInstance().build.asString()
          val plugin = RPluginUtil.getPlugin()
          val pluginVersion = plugin.getVersion()
          val pluginId = plugin.getPluginId().getIdString()
          val os = URLEncoder.encode(SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION, CharsetToolkit.UTF8)
          val uid = PermanentInstallationID.get()
          val url = "https://plugins.jetbrains.com/plugins/list" +
                    "?pluginId=" + pluginId +
                    "&build=" + buildNumber +
                    "&pluginVersion=" + pluginVersion +
                    "&os=" + os +
                    "&uuid=" + uid
          PropertiesComponent.getInstance().setValue(KEY, System.currentTimeMillis().toString())
          HttpRequests.request(url).connect<Any> { request ->
            try {
              JDOMUtil.load(request.reader)
            }
            catch (ignore: JDOMException) {
            }

            null
          }
        }
        catch (ignored: Exception) {
        }
      }
    }
  }
}