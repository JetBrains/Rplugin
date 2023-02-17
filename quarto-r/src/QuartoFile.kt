import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import org.jetbrains.quarto.QuartoFileType

class QuartoFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, QuartoLanguage) {
  override fun getFileType() = QuartoFileType
}