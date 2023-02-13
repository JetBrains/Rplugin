import com.intellij.formatting.ChildAttributes
import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.tree.IElementType

interface QmdFenceProvider {
  val fenceLanguage: Language

  /** Element type in Markdown language to identify Markdown token with executable code (without header and backticks) */
  val fenceElementType: IElementType

  /** Indent and alignment settings which are applied to a new child block inside Code Fence */
  fun getNewChildAttributes(newChildIndex: Int): ChildAttributes

  companion object {
    val EP_NAME: ExtensionPointName<QmdFenceProvider> = ExtensionPointName.create<QmdFenceProvider>("com.intellij.qmdFenceProvider")

    fun find(predicate: (QmdFenceProvider) -> Boolean): QmdFenceProvider? {
      return EP_NAME.extensionList.firstOrNull(predicate)
    }

    fun matchHeader(fullFenceHeader: CharSequence): QmdFenceProvider? {
      val language = QuartoPsiUtil.getExecutableFenceLanguage(fullFenceHeader) ?: return null
      return find { language.toLowerCase() == it.fenceLanguage.displayName.toLowerCase() }
    }
  }
}
