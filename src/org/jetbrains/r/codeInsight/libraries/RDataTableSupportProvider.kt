package org.jetbrains.r.codeInsight.libraries

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.util.Processor
import org.jetbrains.r.codeInsight.table.RDataTableContextManager
import org.jetbrains.r.editor.completion.RLookupElementFactory
import org.jetbrains.r.psi.TableColumnInfo
import org.jetbrains.r.psi.api.RIdentifierExpression
import org.jetbrains.r.psi.api.RPsiElement
import org.jetbrains.r.psi.isInsideSubscription

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