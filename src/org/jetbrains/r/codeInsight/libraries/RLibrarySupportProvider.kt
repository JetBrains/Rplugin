package org.jetbrains.r.codeInsight.libraries

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.ResolveResult
import org.jetbrains.r.editor.completion.RLookupElementFactory
import org.jetbrains.r.psi.api.RPsiElement

interface RLibrarySupportProvider {
  companion object {
    val EP_NAME = ExtensionPointName.create<RLibrarySupportProvider>("org.jetbrains.r.librarySupportProvider")
  }

  /**
   * Implement this method to resolve element according to the custom logic of the library
   */
  fun resolve(element: RPsiElement): ResolveResult?

  /**
   * Implement this method to provide member access completion, i.e. completion of `member` in expression `var$member`
   */
  fun completeMembers(receiver: RPsiElement, lookupElementFactory: RLookupElementFactory, completionConsumer: CompletionResultSet)
}