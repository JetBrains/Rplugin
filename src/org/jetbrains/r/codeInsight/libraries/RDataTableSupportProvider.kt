package org.jetbrains.r.codeInsight.libraries

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.r.psi.codeInsight.libraries.RLibrarySupportProvider
import com.intellij.r.psi.editor.completion.RLookupElementFactory
import com.intellij.r.psi.psi.TableColumnInfo
import com.intellij.r.psi.psi.api.RIdentifierExpression
import com.intellij.r.psi.psi.api.RPsiElement
import com.intellij.r.psi.psi.isInsideSubscription
import com.intellij.util.Processor
import org.jetbrains.r.codeInsight.table.RDataTableContextManager

class RDataTableSupportProvider : RLibrarySupportProvider{
  override fun resolve(element: RPsiElement): ResolveResult? {
    if (element !is RIdentifierExpression || !element.isInsideSubscription) return null

    val resultElementRef = Ref<PsiElement>()
    val resolveProcessor = object : Processor<TableColumnInfo> {
      override fun process(it: TableColumnInfo): Boolean {
        if (it.name == element.name) {
          resultElementRef.set(it.definition)
          return false
        }
        return true
      }
    }

    RDataTableContextManager().processColumnsInContext(element, resolveProcessor)
    if (!resultElementRef.isNull) {
      return PsiElementResolveResult(resultElementRef.get())
    }
    return null
  }

  override fun completeMembers(receiver: RPsiElement,
                               lookupElementFactory: RLookupElementFactory,
                               completionConsumer: CompletionResultSet) {
  }

  override fun completeIdentifier(element: PsiElement,
                                  lookupElementFactory: RLookupElementFactory,
                                  completionConsumer: CompletionResultSet) {}
}