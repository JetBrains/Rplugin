package org.jetbrains.r.documentation

import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.ElementBase
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.r.psi.psi.api.RStringLiteralExpression

class RDocumentationFakeTargetElement(private val rExpression: RStringLiteralExpression) : ElementBase(), PsiNamedElement {
  override fun getProject(): Project {return rExpression.project}

  override fun getLanguage(): Language {return rExpression.language}

  override fun getManager(): PsiManager {return rExpression.manager}

  override fun getChildren(): Array<PsiElement> {return arrayOf()}

  override fun getParent(): PsiElement? {
    return null
  }

  override fun getFirstChild(): PsiElement? {
    return null
  }

  override fun getLastChild(): PsiElement? {return null}

  override fun getNextSibling(): PsiElement? {return null}

  override fun getPrevSibling(): PsiElement? {return null}

  override fun getContainingFile(): PsiFile? {return rExpression.containingFile}

  override fun getTextRange(): TextRange? {return null}

  override fun getStartOffsetInParent(): Int {return 0}

  override fun getTextLength(): Int {return rExpression.name?.length ?: 0}

  override fun findElementAt(offset: Int): PsiElement? {return null}

  override fun findReferenceAt(offset: Int): PsiReference? {return null}

  override fun getTextOffset(): Int {return 0}

  override fun getText(): String {return rExpression.text}

  override fun textToCharArray(): CharArray {return rExpression.textToCharArray()}

  override fun getNavigationElement(): PsiElement? {return null}

  override fun getOriginalElement(): PsiElement? {return null}

  override fun textMatches(text: CharSequence): Boolean {return rExpression.textMatches(text)}

  override fun textMatches(element: PsiElement): Boolean {return rExpression.textMatches(element)}

  override fun textContains(c: Char): Boolean {return rExpression.textContains(c)}

  override fun accept(visitor: PsiElementVisitor) {}

  override fun acceptChildren(visitor: PsiElementVisitor) {}

  override fun copy(): PsiElement {return clone() as PsiElement}

  override fun add(element: PsiElement): PsiElement {throw UnsupportedOperationException(javaClass.name)}

  override fun addBefore(element: PsiElement, anchor: PsiElement?): PsiElement {throw UnsupportedOperationException(javaClass.name)}

  override fun addAfter(element: PsiElement, anchor: PsiElement?): PsiElement {throw UnsupportedOperationException(javaClass.name)}

  override fun checkAdd(element: PsiElement) {throw UnsupportedOperationException(javaClass.name)}

  override fun addRange(first: PsiElement?, last: PsiElement?): PsiElement {throw UnsupportedOperationException(javaClass.name)}

  override fun addRangeBefore(first: PsiElement, last: PsiElement, anchor: PsiElement?): PsiElement {throw UnsupportedOperationException(javaClass.name)}

  override fun addRangeAfter(first: PsiElement?, last: PsiElement?, anchor: PsiElement?): PsiElement {throw UnsupportedOperationException(javaClass.name)}

  override fun delete() {}

  override fun checkDelete() {}

  override fun deleteChildRange(first: PsiElement?, last: PsiElement?) {}

  override fun replace(newElement: PsiElement): PsiElement {return this}

  override fun isValid(): Boolean {return true}

  override fun isWritable(): Boolean {return false}

  override fun getReference(): PsiReference? {return null}

  override fun getReferences(): Array<PsiReference> {return arrayOf()}

  override fun processDeclarations(processor: PsiScopeProcessor, state: ResolveState, lastParent: PsiElement?, place: PsiElement): Boolean {
    return rExpression.processDeclarations(processor, state, lastParent, place)
  }

  override fun getContext(): PsiElement? {return rExpression.context}

  override fun isPhysical(): Boolean {return false}

  override fun getResolveScope(): GlobalSearchScope {return rExpression.resolveScope}

  override fun getUseScope(): SearchScope {return rExpression.useScope}

  override fun getNode(): ASTNode {return rExpression.node}

  override fun isEquivalentTo(another: PsiElement?): Boolean {return rExpression.isEquivalentTo(another)}

  override fun getName(): String? {
    return rExpression.name
  }

  override fun setName(name: String): PsiElement {return this}
}