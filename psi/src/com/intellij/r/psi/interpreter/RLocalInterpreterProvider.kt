package com.intellij.r.psi.interpreter

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

interface RLocalInterpreterProvider {
  fun instantiate(location: RLocalInterpreterLocation, project: Project): RInterpreterBase

  companion object {
    fun getInstance(project: Project): RLocalInterpreterProvider = project.service()

    val LOG = Logger.getInstance(RLocalInterpreterProvider::class.java)
  }
}