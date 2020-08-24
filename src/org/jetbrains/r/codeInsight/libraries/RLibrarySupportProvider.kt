package org.jetbrains.r.codeInsight.libraries

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.ResolveResult
import org.jetbrains.r.psi.api.RPsiElement

interface RLibrarySupportProvider {
  companion object {
    val EP_NAME = ExtensionPointName.create<RLibrarySupportProvider>("org.jetbrains.r.librarySupportProvider")
  }

  fun resolve(element: RPsiElement): ResolveResult?
}