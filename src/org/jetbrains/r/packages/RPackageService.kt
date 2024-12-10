// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * @author avesloguzova
 * @author holgerbrandl
 */
@Service(Service.Level.PROJECT)
@State(name = "RPackageService", storages = [Storage(value = "rpackages.xml")])
class RPackageService : PersistentStateComponent<RPackageService> {
  var cranMirror = 0
  var enabledRepositoryUrls = mutableListOf<String>()
  var userRepositoryUrls = mutableListOf<String>()

  override fun getState(): RPackageService {
    return this
  }

  override fun loadState(state: RPackageService) {
    XmlSerializerUtil.copyBean(state, this)
  }

  companion object {
    fun getInstance(project: Project): RPackageService {
      return project.service()
    }
  }
}
