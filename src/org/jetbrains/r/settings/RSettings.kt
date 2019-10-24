// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.runInEdtAndWait
import org.jdom.Element
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.interpreter.RInterpreterUtil
import java.io.File

@State(name = "RSettings", storages = [Storage("rSettings.xml")])
class RSettings(private val project: Project) : PersistentStateComponent<Element> {
  private var backingPath: String? = null

  var interpreterPath: String
    get() {
      fun getSuggestedPath(): String {
        runInEdtAndWait {  }
        return runAsync { RInterpreterUtil.suggestHomePath() }.
          onError { Logger.getInstance(RSettings::class.java).error(it) }.
          blockingGet(RInterpreterUtil.EDT_TIMEOUT) ?: ""
      }

      return backingPath ?: getSuggestedPath().also {
        backingPath = it
      }
    }
    set(value) {
      backingPath = value
    }

  override fun getState(): Element? {
    val root = Element("rsettings")
    root.setAttribute("path", interpreterPath)
    return root
  }

  override fun loadState(state: Element) {
    val path = state.getAttributeValue("path")
    if (!path.isNullOrBlank()) {
      interpreterPath = path
    }
  }

  companion object {
    fun getInstance(project: Project): RSettings = project.getComponent(RSettings::class.java) ?: RSettings(project)


    fun hasInterpreter(project: Project): Boolean {
      val interpreterPath = RSettings.getInstance(project).interpreterPath
      return !StringUtil.isEmptyOrSpaces(interpreterPath) && File(interpreterPath).canExecute()
    }
  }
}
