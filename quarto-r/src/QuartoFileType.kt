import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

object QuartoFileType : LanguageFileType(QuartoLanguage) {
  override fun getName() = "Quarto"
  @NonNls
  override fun getDescription() = "Quarto"
  override fun getDefaultExtension() = "Qmd"
  override fun getIcon(): Icon = AllIcons.Actions.Stub
  override fun getCharset(file: VirtualFile, content: ByteArray): String = CharsetToolkit.UTF8
}